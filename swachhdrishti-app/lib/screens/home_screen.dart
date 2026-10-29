import 'package:flutter/material.dart';

import '../api.dart';
import '../models.dart';
import 'report_screen.dart';
import 'reports_screen.dart';
import 'rewards_screen.dart';

/// Bottom-navigation shell: Report · My reports · Rewards.
class HomeScreen extends StatefulWidget {
  final Future<void> Function() onLogout;
  const HomeScreen({super.key, required this.onLogout});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;
  Profile? _profile;
  List<Complaint> _complaints = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([Api.me(), Api.myComplaints()]);
      if (!mounted) return;
      setState(() {
        _profile = results[0] as Profile;
        _complaints = results[1] as List<Complaint>;
      });
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await widget.onLogout();
        return;
      }
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      ReportScreen(onSubmitted: _load, onSessionExpired: widget.onLogout),
      ReportsScreen(complaints: _complaints, loading: _loading, error: _error, onRefresh: _load),
      RewardsScreen(profile: _profile, onRefresh: _load, onLogout: widget.onLogout),
    ];
    return Scaffold(
      appBar: AppBar(
        title: const Text('SwachhDrishti'),
        actions: [
          if (_profile != null)
            Padding(
              padding: const EdgeInsets.only(right: 12),
              child: Chip(
                avatar: const Text('🪙'),
                label: Text('${_profile!.cleanCoins}'),
                visualDensity: VisualDensity.compact,
              ),
            ),
        ],
      ),
      body: IndexedStack(index: _tab, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (i) => setState(() => _tab = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.add_a_photo_outlined), selectedIcon: Icon(Icons.add_a_photo), label: 'Report'),
          NavigationDestination(icon: Icon(Icons.list_alt_outlined), selectedIcon: Icon(Icons.list_alt), label: 'My reports'),
          NavigationDestination(icon: Icon(Icons.emoji_events_outlined), selectedIcon: Icon(Icons.emoji_events), label: 'Rewards'),
        ],
      ),
    );
  }
}
