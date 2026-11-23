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
    expect(c.statusText, 'Reopened');
    expect(c.isOpen, isTrue);
    expect(c.isDone, isFalse);
    expect(c.slaDueAt, isNotNull);
    expect(c.coveragePct, closeTo(63.4, 1e-9));
  });

  test('Profile defaults missing numbers to zero', () {
    final p = Profile.fromJson({'name': 'Asha', 'email': 'a@x.in'});
    expect(p.cleanCoins, 0);
    expect(p.totalReports, 0);
  });

  test('Reward levels follow the coin balance', () {
    expect(rewardLevel(0).name, 'Green Starter');
    expect(rewardLevel(29).name, 'Green Starter');
    expect(rewardLevel(30).name, 'Eco Hero');
    expect(rewardLevel(99).name, 'Eco Hero');
    expect(rewardLevel(100).name, 'City Champion');
  });

  test('Severity words and status labels', () {
    expect(severityWord(1), 'Minor');
    expect(severityWord(5), 'Critical');
    expect(statusLabel('CLEANED'), 'Cleaned');
    expect(statusLabel('PENDING'), 'Received');
  });
}
