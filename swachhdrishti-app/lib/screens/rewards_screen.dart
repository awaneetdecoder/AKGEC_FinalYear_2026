import 'package:flutter/material.dart';

import '../api.dart';
import '../models.dart';
import '../theme.dart';
import '../widgets.dart';

/// Clean Coins, level, recent cleanups, how rewards work, settings and sign-out.
class RewardsScreen extends StatelessWidget {
  final Profile? profile;
  final List<Complaint> complaints;
  final Future<void> Function() onRefresh;
  final Future<void> Function() onLogout;

  const RewardsScreen({
    super.key,
    required this.profile,
    required this.complaints,
    required this.onRefresh,
    required this.onLogout,
  });

  Widget _step(BuildContext context, int n, String title, String body) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 30,
          height: 30,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: scheme.primaryContainer, shape: BoxShape.circle),
          child: Text('$n', style: TextStyle(fontWeight: FontWeight.w800, color: scheme.onPrimaryContainer)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(body, style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 13, height: 1.4)),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _statTile(BuildContext context, IconData icon, String value, String label) {
    final scheme = Theme.of(context).colorScheme;
    return SoftCard(
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(icon, color: scheme.primary),
        const SizedBox(height: 10),
        Text(value, style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w800)),
        Text(label, style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 13)),
      ]),
    );
  }

  Future<void> _confirmLogout(BuildContext context) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sign out?'),
        content: const Text('You can sign back in any time with the same e-mail and password.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: FilledButton.styleFrom(minimumSize: const Size(96, 44)),
            child: const Text('Sign out'),
          ),
        ],
      ),
    );
    if (ok == true) await onLogout();
  }

  Future<void> _editServer(BuildContext context) async {
    final changed = await showDialog<bool>(context: context, builder: (_) => const _ServerDialog());
    if (changed == true) {
      await onRefresh();
      if (context.mounted) showAppSnack(context, 'Server address updated');
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final p = profile;
    final coins = p?.cleanCoins ?? 0;
    final lv = rewardLevel(coins);
    final maxed = coins >= 100;
    final progress = maxed ? 1.0 : (coins - lv.from) / (lv.next - lv.from);
    final cleaned = complaints.where((c) => c.isDone).toList();
    final name = (p?.name ?? Session.name ?? '').trim();
    final initial = name.isEmpty ? '?' : String.fromCharCode(name.runes.first).toUpperCase();

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
        children: [
          // ----- balance hero -----
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(28),
              gradient: kBrandGradient,
              boxShadow: [
                BoxShadow(color: kBrandSeed.withAlpha(80), blurRadius: 22, offset: const Offset(0, 10)),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(color: Colors.white.withAlpha(46), borderRadius: BorderRadius.circular(999)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.workspace_premium, color: Color(0xFFF2B01E), size: 18),
                    const SizedBox(width: 6),
                    Text(lv.name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                  ]),
                ),
                const SizedBox(height: 16),
                Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                  const Text('🪙', style: TextStyle(fontSize: 38)),
                  const SizedBox(width: 10),
                  Text('$coins',
                      style: const TextStyle(color: Colors.white, fontSize: 52, fontWeight: FontWeight.w900, height: 1)),
                  const SizedBox(width: 8),
                  Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: Text('Clean Coins', style: TextStyle(color: Colors.white.withAlpha(225), fontSize: 15)),
                  ),
                ]),
                const SizedBox(height: 18),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: LinearProgressIndicator(
                    value: progress.clamp(0.0, 1.0),
                    minHeight: 9,
                    backgroundColor: Colors.white.withAlpha(60),
                    valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFFF2B01E)),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  maxed
                      ? 'Top level reached. Thank you for keeping the city clean!'
                      : '${lv.next - coins} more coins to the next level',
                  style: TextStyle(color: Colors.white.withAlpha(225), fontSize: 13),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          Row(children: [
            Expanded(child: _statTile(context, Icons.assignment_turned_in_outlined, '${p?.totalReports ?? 0}', 'Reports filed')),
            const SizedBox(width: 10),
            Expanded(child: _statTile(context, Icons.cleaning_services_outlined, '${cleaned.length}', 'Spots cleaned')),
          ]),
          const SizedBox(height: 14),

          // ----- recent cleanups -----
          SoftCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SectionTitle(icon: Icons.history_rounded, text: 'Recent cleanups'),
                const SizedBox(height: 10),
                if (cleaned.isEmpty)
                  Text('No verified cleanups yet. Once a worker confirms one of your reports on site, it appears here with your coins.',
                      style: TextStyle(color: scheme.onSurfaceVariant, height: 1.4))
                else
                  for (final c in cleaned.take(5))
                    ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      leading: CircleAvatar(
                        radius: 18,
                        backgroundColor: severityColor(c.severity).withAlpha(40),
                        child: Text('${c.severity}',
                            style: TextStyle(fontWeight: FontWeight.w800, color: severityColor(c.severity))),
                      ),
                      title: Text('${c.garbageType} · #${c.id}', style: const TextStyle(fontWeight: FontWeight.w700)),
                      subtitle: Text(timeAgo(c.createdAt)),
                      trailing: Text('+$kCoinsPerCleanup 🪙',
                          style: TextStyle(fontWeight: FontWeight.w800, color: scheme.primary)),
                    ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // ----- how it works -----
          SoftCard(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SectionTitle(icon: Icons.lightbulb_outline_rounded, text: 'How you earn'),
                const SizedBox(height: 16),
                _step(context, 1, 'Report', 'File a report with a clear photo and your location.'),
                const SizedBox(height: 14),
                _step(context, 2, 'Verified cleanup',
                    'A worker cleans it and confirms on site. The system checks their GPS against an authorised disposal zone.'),
                const SizedBox(height: 14),
                _step(context, 3, 'Get rewarded', 'You receive $kCoinsPerCleanup Clean Coins once that cleanup is verified.'),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: scheme.primaryContainer.withAlpha(90),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Icon(Icons.verified_user_outlined, size: 18, color: scheme.primary),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text('Coins are only paid for genuinely verified cleanups, and never twice for the same report.',
                          style: TextStyle(fontSize: 12.5, height: 1.4, color: scheme.onSurfaceVariant)),
                    ),
                  ]),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // ----- account & settings -----
          SoftCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Column(children: [
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: CircleAvatar(
                  radius: 24,
                  backgroundColor: scheme.primaryContainer,
                  child: Text(initial,
                      style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18, color: scheme.onPrimaryContainer)),
                ),
                title: Text(name.isEmpty ? 'Citizen' : name, style: const TextStyle(fontWeight: FontWeight.w800)),
                subtitle: Text(p?.email ?? Session.email ?? ''),
              ),
              Divider(height: 1, color: scheme.outlineVariant.withAlpha(140)),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.dns_outlined, color: scheme.primary),
                title: const Text('Server address'),
                subtitle: Text(Session.baseUrl, maxLines: 1, overflow: TextOverflow.ellipsis),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () => _editServer(context),
              ),
              Divider(height: 1, color: scheme.outlineVariant.withAlpha(140)),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.logout_rounded, color: scheme.error),
                title: Text('Sign out', style: TextStyle(color: scheme.error, fontWeight: FontWeight.w700)),
                onTap: () => _confirmLogout(context),
              ),
            ]),
          ),
        ],
      ),
    );
  }
}

/// Edit and test the server address (handy when the PC's IP changes).
class _ServerDialog extends StatefulWidget {
  const _ServerDialog();

  @override
  State<_ServerDialog> createState() => _ServerDialogState();
}

class _ServerDialogState extends State<_ServerDialog> {
  final _controller = TextEditingController(text: Session.baseUrl);
  bool _testing = false;
  String? _note;
  bool _ok = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _test() async {
    setState(() {
      _testing = true;
      _note = null;
    });
    final ok = await Api.ping(_controller.text);
    if (!mounted) return;
    setState(() {
      _testing = false;
      _ok = ok;
      _note = ok ? 'Server reachable' : 'Cannot reach this address';
    });
  }

  Future<void> _save() async {
    final url = _controller.text.trim();
    if (!url.startsWith('http')) {
      setState(() {
        _ok = false;
        _note = 'Must start with http:// or https://';
      });
      return;
    }
    await Session.setBaseUrl(url);
    if (mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return AlertDialog(
      title: const Text('Server address'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: _controller,
            keyboardType: TextInputType.url,
            decoration: const InputDecoration(
              labelText: 'Address',
              helperText: 'USB: http://127.0.0.1:8080\nEmulator: http://10.0.2.2:8080\nWi-Fi: http://<PC-IP>:8080',
              helperMaxLines: 3,
            ),
          ),
          if (_note != null)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Text(_note!,
                  style: TextStyle(fontWeight: FontWeight.w600, color: _ok ? scheme.primary : scheme.error)),
            ),
        ],
      ),
      actions: [
        TextButton(onPressed: _testing ? null : _test, child: Text(_testing ? 'Testing…' : 'Test')),
        TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancel')),
        FilledButton(
          onPressed: _save,
          style: FilledButton.styleFrom(minimumSize: const Size(80, 44)),
          child: const Text('Save'),
        ),
      ],
    );
  }
}
