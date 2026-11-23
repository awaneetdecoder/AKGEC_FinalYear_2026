import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'models.dart';

class ApiException implements Exception {
  final String message;
  final int? status;
  ApiException(this.message, [this.status]);

  bool get isUnauthorized => status == 401;

  @override
  String toString() => message;
}

/// Login state, kept in memory and mirrored to SharedPreferences.
class Session {
  /// 10.0.2.2 reaches the PC from the Android emulator. A phone on USB uses
  /// http://127.0.0.1:8080 after `adb reverse tcp:8080 tcp:8080`. A phone on
  /// Wi-Fi uses the PC's address, e.g. http://192.168.1.20:8080. The app finds
  /// the right one by itself (see Api.findServer).
  static const defaultBaseUrl = 'http://10.0.2.2:8080';

  static String baseUrl = defaultBaseUrl;
  static String? token;
  static String? name;
  static String? email;

  static bool get isLoggedIn => token != null;

  /// First word of the user's name, for greetings.
  static String get firstName {
    final n = (name ?? '').trim();
    if (n.isEmpty) return 'there';
    return n.split(RegExp(r'\s+')).first;
  }

  static Future<void> load() async {
    final p = await SharedPreferences.getInstance();
    baseUrl = p.getString('baseUrl') ?? defaultBaseUrl;
    token = p.getString('token');
    name = p.getString('name');
    email = p.getString('email');
  }

  static Future<void> save() async {
    final p = await SharedPreferences.getInstance();
    await p.setString('baseUrl', baseUrl);
    if (token != null) await p.setString('token', token!);
    if (name != null) await p.setString('name', name!);
    if (email != null) await p.setString('email', email!);
  }

  /// Changes the server address and remembers it.
  static Future<void> setBaseUrl(String url) async {
    baseUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
    final p = await SharedPreferences.getInstance();
    await p.setString('baseUrl', baseUrl);
  }

  static Future<void> clear() async {
    token = null;
    name = null;
    email = null;
    final p = await SharedPreferences.getInstance();
    await p.remove('token');
    await p.remove('name');
    await p.remove('email');
  }
}

/// Thin client for the Spring Boot REST API.
class Api {
  static const _timeout = Duration(seconds: 20);

  static Future<dynamic> _request(String method, String path, {Object? body}) async {
    final uri = Uri.parse('${Session.baseUrl}$path');
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (Session.token != null) 'Authorization': 'Bearer ${Session.token}',
    };
    try {
      final http.Response res = method == 'POST'
          ? await http.post(uri, headers: headers, body: body == null ? null : jsonEncode(body)).timeout(_timeout)
          : await http.get(uri, headers: headers).timeout(_timeout);
      return await _decode(res);
    } on TimeoutException {
      throw ApiException(_unreachable());
    } on SocketException {
      throw ApiException(_unreachable());
    } on http.ClientException {
      throw ApiException(_unreachable());
    }
  }

  /// A message that says what to do, not just that it failed.
  static String _unreachable() =>
      'Cannot reach the server at ${Session.baseUrl}.\n'
      '• USB phone: run  adb reverse tcp:8080 tcp:8080  and use http://127.0.0.1:8080\n'
      '• Wi-Fi phone: use your PC\'s IP, e.g. http://192.168.1.20:8080\n'
      '• Emulator: use http://10.0.2.2:8080\n'
      'Make sure the backend is running.';

  static Future<dynamic> _decode(http.Response res) async {
    dynamic data;
    if (res.body.isNotEmpty) {
      try {
        data = jsonDecode(utf8.decode(res.bodyBytes));
      } catch (_) {
        data = null;
      }
    }
    if (res.statusCode >= 400) {
      if (res.statusCode == 401 && Session.token != null) {
        await Session.clear();
        throw ApiException('Your session expired. Please sign in again.', 401);
      }
      final msg = data is Map && data['message'] is String ? data['message'] as String : 'Request failed (${res.statusCode}).';
      throw ApiException(msg, res.statusCode);
    }
    return data;
  }

  /// True if the server at [baseUrl] answers its health check.
  static Future<bool> ping(String baseUrl, {Duration timeout = const Duration(seconds: 6)}) async {
    try {
      final url = baseUrl.trim().replaceAll(RegExp(r'/+$'), '');
      final res = await http.get(Uri.parse('$url/actuator/health')).timeout(timeout);
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  /// Tries the saved address, the USB address and the emulator address together
  /// and returns the first that answers, or null if none does.
  static Future<String?> findServer() async {
    final candidates = <String>{Session.baseUrl, 'http://127.0.0.1:8080', 'http://10.0.2.2:8080'}.toList();
    final results = await Future.wait(
      candidates.map((u) => ping(u, timeout: const Duration(seconds: 3))),
    );
    for (var i = 0; i < candidates.length; i++) {
      if (results[i]) return candidates[i];
    }
    return null;
  }

  // ---- auth ----

  /// Sign in with the e-mail and password used at sign-up.
  static Future<void> login({required String email, required String password}) async {
    final data = await _request('POST', '/api/auth/login', body: {
      'email': email,
      'password': password,
    }) as Map<String, dynamic>;
    await _startSession(data);
  }

  /// Create a citizen account and sign in. (Workers and officers register on the web portal.)
  static Future<void> register({required String name, required String email, required String password}) async {
    final data = await _request('POST', '/api/auth/register', body: {
      'name': name,
      'email': email,
      'password': password,
      'role': 'CITIZEN',
      'accessCode': '',
    }) as Map<String, dynamic>;
    await _startSession(data);
  }

  static Future<void> _startSession(Map<String, dynamic> data) async {
    final role = (data['role'] as String?) ?? '';
    if (role != 'CITIZEN') {
      throw ApiException('This is a staff account. Please sign in on the web portal instead.');
    }
    Session.token = data['token'] as String;
    Session.name = data['name'] as String?;
    Session.email = data['email'] as String?;
    await Session.save();
  }

  static Future<Profile> me() async =>
      Profile.fromJson(await _request('GET', '/api/auth/me') as Map<String, dynamic>);

  // ---- complaints ----
  static Future<List<Complaint>> myComplaints() async {
    final list = await _request('GET', '/api/complaints/mine') as List<dynamic>;
    return list.map((e) => Complaint.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Uploads the photo, then files the complaint with its URL + GPS.
  static Future<Complaint> submitReport({
    required File photo,
    required double latitude,
    required double longitude,
  }) async {
    final url = await uploadPhoto(photo);
    final data = await _request('POST', '/api/complaints', body: {
      'photoUrl': url,
      'latitude': latitude,
      'longitude': longitude,
    }) as Map<String, dynamic>;
    return Complaint.fromJson(data);
  }

  static Future<String> uploadPhoto(File file) async {
    try {
      final req = http.MultipartRequest('POST', Uri.parse('${Session.baseUrl}/api/uploads'));
      req.headers['Authorization'] = 'Bearer ${Session.token}';
      req.files.add(await http.MultipartFile.fromPath('file', file.path, contentType: _mediaType(file.path)));
      final streamed = await req.send().timeout(const Duration(seconds: 60));
      final res = await http.Response.fromStream(streamed);
      final data = await _decode(res) as Map<String, dynamic>;
      return data['url'] as String;
    } on TimeoutException {
      throw ApiException('Photo upload timed out. Check the connection to the server and try again.');
    } on SocketException {
      throw ApiException(_unreachable());
    } on http.ClientException {
      throw ApiException(_unreachable());
    }
  }

  static MediaType _mediaType(String path) {
    final p = path.toLowerCase();
    if (p.endsWith('.png')) return MediaType('image', 'png');
    if (p.endsWith('.webp')) return MediaType('image', 'webp');
    return MediaType('image', 'jpeg');
  }
}
