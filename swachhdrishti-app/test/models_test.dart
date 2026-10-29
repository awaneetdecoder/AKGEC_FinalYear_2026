import 'package:flutter_test/flutter_test.dart';
import 'package:swachhdrishti_app/models.dart';

void main() {
  test('Complaint parses the API response', () {
    final c = Complaint.fromJson({
      'complaintId': 7,
      'status': 'REOPENED',
      'garbageType': 'Plastic',
      'severity': 5,
      'slaDeadlineHours': 0,
      'slaDueAt': '2026-10-26T10:15:30.123456',
      'slaBreached': true,
      'boundingBoxAreaPct': 63.4,
      'needsReview': false,
      'latitude': 28.6692,
      'longitude': 77.4538,
      'photoUrl': 'http://10.0.2.2:8080/uploads/a.jpg',
      'createdAt': '2026-10-26T09:15:30.123456',
    });
    expect(c.id, 7);
    expect(c.slaLabel, 'Immediate dispatch');
    expect(c.statusLabel, 'Reopened');
    expect(c.slaDueAt, isNotNull);
    expect(c.coveragePct, closeTo(63.4, 1e-9));
  });

  test('Profile defaults missing numbers to zero', () {
    final p = Profile.fromJson({'name': 'Asha', 'email': 'a@x.in'});
    expect(p.cleanCoins, 0);
    expect(p.totalReports, 0);
  });
}
