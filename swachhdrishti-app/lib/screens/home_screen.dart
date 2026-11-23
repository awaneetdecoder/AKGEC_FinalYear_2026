import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api.dart';
import '../models.dart';
import '../theme.dart';
import '../widgets.dart';
import 'report_screen.dart';
import 'reports_screen.dart';
import 'rewards_screen.dart';

/// App shell: gradient header with the coin balance, live data, bottom navigation.
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
  bool _refreshing = false;
  String? _error;
  Map<int, String> _previous = {};
  Timer? _timer;

  // Short caption shown under the app title for each tab.
  static const List<String> _subtitles = [
    'Report a civic issue',
    'Track your submissions',
    'Your rewards & settings',
  ];

  @override
  void initState() {
    super.initState();
    _load();
    // Quietly refresh every 30 s so status changes show up without pulling down.
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => _load(silent: true));
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (_refreshing) return;
    _refreshing = true;
    if (!silent) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final results = await Future.wait([Api.me(), Api.myComplaints()]);
      if (!mounted) return;
      final fresh = results[1] as List<Complaint>;
      _announceChanges(fresh);
      setState(() {
        _profile = results[0] as Profile;
        _complaints = fresh;
        _error = null;
      });
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await widget.onLogout();
        return;
      }
      if (mounted && !silent) setState(() => _error = e.message);
    } finally {
      _refreshing = false;
      if (mounted && !silent) setState(() => _loading = false);
    }
  }

  /// Tells the user when one of their reports changed status since the last refresh.
  void _announceChanges(List<Complaint> fresh) {
    if (_previous.isNotEmpty) {
      for (final c in fresh) {
        final old = _previous[c.id];
        if (old == null || old == c.status) continue;
        String? msg;
        if (c.status == 'CLEANED') {
          msg = 'Report #${c.id} was cleaned! 🪙 +$kCoinsPerCleanup Clean Coins';
        } else if (c.status == 'ASSIGNED') {
          msg = 'A worker picked up report #${c.id}.';
        } else if (c.status == 'REOPENED') {
          msg = 'Report #${c.id} was reopened and prioritised.';
        }
        if (msg != null) {
          HapticFeedback.mediumImpact();
          showAppSnack(context, msg);
          break;
        }
      }
    }
    _previous = {for (final c in fresh) c.id: c.status};
  }

  // ---------------------------------------------------------------------------
  // UI
  // ---------------------------------------------------------------------------

  PreferredSizeWidget _buildAppBar(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return AppBar(
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      foregroundColor: Colors.white,
      backgroundColor: Colors.transparent,
      systemOverlayStyle: SystemUiOverlayStyle.light,
      toolbarHeight: 68,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
      ),
      flexibleSpace: Container(
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.vertical(bottom: Radius.circular(24)),
          gradient: kBrandGradient,
          boxShadow: [
            BoxShadow(color: kBrandSeed.withAlpha(70), blurRadius: 14, offset: const Offset(0, 4)),
          ],
        ),
      ),
      titleSpacing: 16,
      title: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(color: Colors.white.withAlpha(45), borderRadius: BorderRadius.circular(12)),
            child: const Icon(Icons.eco_rounded, color: Colors.white, size: 24),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'SwachhDrishti',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, letterSpacing: 0.3, color: Colors.white),
                ),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: Text(
                    _subtitles[_tab],
                    key: ValueKey(_tab),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.white.withAlpha(215)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      actions: [
        if (_profile != null)
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Tooltip(
              message: 'Your Clean Coins',
              child: GestureDetector(
                onTap: () => setState(() => _tab = 2),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                  decoration: BoxDecoration(
                    color: scheme.surface,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(color: Colors.black.withAlpha(35), blurRadius: 8, offset: const Offset(0, 2)),
                    ],
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('🪙', style: TextStyle(fontSize: 16)),
                      const SizedBox(width: 6),
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 300),
                        transitionBuilder: (child, anim) =>
                            ScaleTransition(scale: anim, child: FadeTransition(opacity: anim, child: child)),
                        child: Text(
                          '${_profile!.cleanCoins}',
                          key: ValueKey(_profile!.cleanCoins),
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w800, color: scheme.primary),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildNavBar(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final openCount = _complaints.where((c) => c.isOpen).length;

    return Container(
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        boxShadow: [
          BoxShadow(color: Colors.black.withAlpha(28), blurRadius: 18, offset: const Offset(0, -4)),
        ],
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        child: NavigationBar(
          height: 68,
          elevation: 0,
          backgroundColor: scheme.surface,
          surfaceTintColor: Colors.transparent,
          indicatorColor: scheme.primaryContainer,
          labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
          selectedIndex: _tab,
          onDestinationSelected: (i) {
            if (i == _tab) return;
            HapticFeedback.selectionClick();
            setState(() => _tab = i);
          },
          destinations: [
            const NavigationDestination(
              icon: Icon(Icons.add_a_photo_outlined),
              selectedIcon: Icon(Icons.add_a_photo),
              label: 'Report',
              tooltip: 'Report an issue',
            ),
            NavigationDestination(
              icon: Badge(
                isLabelVisible: openCount > 0,
                label: Text('$openCount'),
                child: const Icon(Icons.list_alt_outlined),
              ),
              selectedIcon: Badge(
                isLabelVisible: openCount > 0,
                label: Text('$openCount'),
                child: const Icon(Icons.list_alt),
              ),
              label: 'My reports',
              tooltip: 'My reports',
            ),
            const NavigationDestination(
              icon: Icon(Icons.emoji_events_outlined),
              selectedIcon: Icon(Icons.emoji_events),
              label: 'Rewards',
              tooltip: 'Rewards',
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      ReportScreen(
        onSubmitted: () => _load(),
        onSessionExpired: widget.onLogout,
        onViewReports: () => setState(() => _tab = 1),
      ),
      ReportsScreen(complaints: _complaints, loading: _loading, error: _error, onRefresh: () => _load()),
      RewardsScreen(
        profile: _profile,
        complaints: _complaints,
        onRefresh: () => _load(),
        onLogout: widget.onLogout,
      ),
    ];

    return Scaffold(
      appBar: _buildAppBar(context),
      body: Column(
        children: [
          if (_error != null && _profile != null)
            _ErrorBanner(message: _error!, onRetry: () => _load()),
          Expanded(child: IndexedStack(index: _tab, children: pages)),
        ],
      ),
      bottomNavigationBar: _buildNavBar(context),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorBanner({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
      color: scheme.errorContainer,
      child: Row(
        children: [
          Icon(Icons.cloud_off_rounded, size: 18, color: scheme.onErrorContainer),
          const SizedBox(width: 10),
          Expanded(
            child: Text(message,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(color: scheme.onErrorContainer, fontSize: 12.5)),
          ),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      ),
    );
  }
}
