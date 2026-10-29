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
  /// 10.0.2.2 is how the Android emulator reaches the computer running the server.
  /// On a real phone, change it on the login screen to e.g. http://192.168.1.20:8080
  static const defaultBaseUrl = 'http://10.0.2.2:8080';

  static String baseUrl = defaultBaseUrl;
  static String? token;
  static String? name;
  static String? email;

  static bool get isLoggedIn => token != null;

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
      throw ApiException('The server took too long to respond. Please try again.');
    } on SocketException {
      throw ApiException('Cannot reach the server at ${Session.baseUrl}. Check the address and your connection.');
    } on http.ClientException {
      throw ApiException('Cannot reach the server at ${Session.baseUrl}. Check the address and your connection.');
    }
  }

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

  // ---- auth ----
  static Future<void> login({required String email, required String name}) async {
    final data = await _request('POST', '/api/auth/login', body: {
      'email': email,
      'name': name,
      'role': 'CITIZEN',
    }) as Map<String, dynamic>;
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
      throw ApiException('Photo upload timed out. Please try again on a better connection.');
    } on SocketException {
      throw ApiException('Cannot reach the server at ${Session.baseUrl}.');
    } on http.ClientException {
      throw ApiException('Cannot reach the server at ${Session.baseUrl}.');
    }
  }

  static MediaType _mediaType(String path) {
    final p = path.toLowerCase();
    if (p.endsWith('.png')) return MediaType('image', 'png');
    if (p.endsWith('.webp')) return MediaType('image', 'webp');
    return MediaType('image', 'jpeg');
  }
}
