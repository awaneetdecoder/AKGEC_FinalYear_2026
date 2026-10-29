import 'package:flutter/material.dart';

import '../api.dart';
import '../models.dart';

/// Clean Coins balance, how rewards work, and sign-out.
class RewardsScreen extends StatelessWidget {
  final Profile? profile;
  final Future<void> Function() onRefresh;
  final Future<void> Function() onLogout;

  const RewardsScreen({super.key, required this.profile, required this.onRefresh, required this.onLogout});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final p = profile;
    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            color: scheme.primaryContainer,
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 28),
              child: Column(
                children: [
                  const Text('🪙', style: TextStyle(fontSize: 44)),
                  Text('${p?.cleanCoins ?? 0}',
                      style: Theme.of(context).textTheme.displayMedium?.copyWith(fontWeight: FontWeight.bold, color: scheme.onPrimaryContainer)),
                  Text('Clean Coins', style: TextStyle(color: scheme.onPrimaryContainer)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Card(
            child: Column(children: [
              ListTile(leading: const Icon(Icons.person_outline), title: Text(p?.name ?? Session.name ?? '—'), subtitle: Text(p?.email ?? Session.email ?? '')),
              const Divider(height: 1),
              ListTile(leading: const Icon(Icons.assignment_outlined), title: const Text('Reports filed'), trailing: Text('${p?.totalReports ?? 0}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
            ]),
          ),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('How you earn', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  const Text('• File a report with a clear photo.\n'
                      '• A worker cleans it and confirms on site: the system checks their GPS against an authorised disposal zone.\n'
                      '• You receive 10 Clean Coins once that cleanup is verified.\n\n'
                      'Coins are only paid for genuinely verified cleanups, and never twice for the same report.'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(onPressed: onLogout, icon: const Icon(Icons.logout), label: const Text('Sign out')),
          const SizedBox(height: 8),
          Center(child: Text('Server: ${Session.baseUrl}', style: TextStyle(fontSize: 11, color: scheme.onSurfaceVariant))),
        ],
      ),
    );
  }
}
