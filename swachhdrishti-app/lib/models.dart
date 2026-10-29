import 'package:flutter/material.dart';

/// A garbage report, as returned by the SwachhDrishti API.
class Complaint {
  final int id;
  final String status;
  final String garbageType;
  final int severity;
  final int slaDeadlineHours;
  final DateTime? slaDueAt;
  final bool slaBreached;
  final double coveragePct;
  final bool needsReview;
  final double latitude;
  final double longitude;
  final String photoUrl;
  final DateTime? createdAt;

  const Complaint({
    required this.id,
    required this.status,
    required this.garbageType,
    required this.severity,
    required this.slaDeadlineHours,
    required this.slaDueAt,
    required this.slaBreached,
    required this.coveragePct,
    required this.needsReview,
    required this.latitude,
    required this.longitude,
    required this.photoUrl,
    required this.createdAt,
  });

  factory Complaint.fromJson(Map<String, dynamic> j) => Complaint(
        id: (j['complaintId'] as num).toInt(),
        status: j['status'] as String? ?? 'PENDING',
        garbageType: j['garbageType'] as String? ?? 'Mixed',
        severity: (j['severity'] as num?)?.toInt() ?? 3,
        slaDeadlineHours: (j['slaDeadlineHours'] as num?)?.toInt() ?? 0,
        slaDueAt: DateTime.tryParse(j['slaDueAt'] as String? ?? ''),
        slaBreached: j['slaBreached'] as bool? ?? false,
        coveragePct: (j['boundingBoxAreaPct'] as num?)?.toDouble() ?? 0,
        needsReview: j['needsReview'] as bool? ?? false,
        latitude: (j['latitude'] as num).toDouble(),
        longitude: (j['longitude'] as num).toDouble(),
        photoUrl: j['photoUrl'] as String? ?? '',
        createdAt: DateTime.tryParse(j['createdAt'] as String? ?? ''),
      );

  /// "48 h", "6 h" ... or "Immediate dispatch" for level 5.
  String get slaLabel => slaDeadlineHours == 0 ? 'Immediate dispatch' : '$slaDeadlineHours h';

  String get statusLabel => switch (status) {
        'PENDING' => 'Received',
        'ASSIGNED' => 'Worker assigned',
        'CLEANED' => 'Cleaned',
        'REOPENED' => 'Reopened',
        'CLOSED' => 'Closed',
        _ => status,
      };
}

/// Logged-in citizen's profile and reward balance.
class Profile {
  final String name;
  final String email;
  final int cleanCoins;
  final int totalReports;

  const Profile({required this.name, required this.email, required this.cleanCoins, required this.totalReports});

  factory Profile.fromJson(Map<String, dynamic> j) => Profile(
        name: j['name'] as String? ?? '',
        email: j['email'] as String? ?? '',
        cleanCoins: (j['cleanCoins'] as num?)?.toInt() ?? 0,
        totalReports: (j['totalReports'] as num?)?.toInt() ?? 0,
      );
}

/// Same 5-colour scale as the Staff Portal.
Color severityColor(int level) => switch (level) {
      1 => const Color(0xFF2E9E5B),
      2 => const Color(0xFF8BB832),
      3 => const Color(0xFFE0A21B),
      4 => const Color(0xFFE8742A),
      _ => const Color(0xFFD6322B),
    };

Color statusColor(String status) => switch (status) {
      'CLEANED' || 'CLOSED' => const Color(0xFF1E8449),
      'REOPENED' => const Color(0xFFC0392B),
      'ASSIGNED' => const Color(0xFF2471A3),
      _ => const Color(0xFFB9770E),
    };
