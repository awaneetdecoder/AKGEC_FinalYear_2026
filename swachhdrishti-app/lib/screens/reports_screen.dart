import 'package:flutter/material.dart';

import '../models.dart';

/// The citizen's own reports with live status.
class ReportsScreen extends StatelessWidget {
  final List<Complaint> complaints;
  final bool loading;
  final String? error;
  final Future<void> Function() onRefresh;

  const ReportsScreen({
    super.key,
    required this.complaints,
    required this.loading,
    required this.error,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: onRefresh,
      child: _body(context),
    );
  }

  Widget _body(BuildContext context) {
    if (loading && complaints.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (error != null && complaints.isEmpty) {
      return ListView(children: [
        const SizedBox(height: 120),
        Icon(Icons.cloud_off, size: 48, color: Theme.of(context).colorScheme.error),
        const SizedBox(height: 12),
        Padding(padding: const EdgeInsets.symmetric(horizontal: 32), child: Text(error!, textAlign: TextAlign.center)),
        const SizedBox(height: 12),
        Center(child: FilledButton(onPressed: onRefresh, child: const Text('Retry'))),
      ]);
    }
    if (complaints.isEmpty) {
      return ListView(children: const [
        SizedBox(height: 120),
        Icon(Icons.eco_outlined, size: 56),
        SizedBox(height: 12),
        Center(child: Text('No reports yet.\nFile your first one from the Report tab!', textAlign: TextAlign.center)),
      ]);
    }
    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: complaints.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (context, i) => _ComplaintTile(complaint: complaints[i]),
    );
  }
}

class _ComplaintTile extends StatelessWidget {
  final Complaint complaint;
  const _ComplaintTile({required this.complaint});

  @override
  Widget build(BuildContext context) {
    final c = complaint;
    final created = c.createdAt;
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        leading: CircleAvatar(
          backgroundColor: severityColor(c.severity),
          child: Text('${c.severity}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        ),
        title: Text('#${c.id} · ${c.garbageType}'),
        subtitle: Text([
          c.statusLabel,
          if (created != null) _fmt(created),
        ].join(' · ')),
        trailing: _StatusDot(status: c.status),
        onTap: () => showModalBottomSheet<void>(context: context, showDragHandle: true, builder: (_) => _Detail(complaint: c)),
      ),
    );
  }

  static String _fmt(DateTime d) {
    final l = d.toLocal();
    const m = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    final hh = l.hour.toString().padLeft(2, '0');
    final mm = l.minute.toString().padLeft(2, '0');
    return '${l.day} ${m[l.month - 1]}, $hh:$mm';
  }
}

class _StatusDot extends StatelessWidget {
  final String status;
  const _StatusDot({required this.status});

  @override
  Widget build(BuildContext context) =>
      Icon(status == 'CLEANED' || status == 'CLOSED' ? Icons.check_circle : Icons.circle, color: statusColor(status), size: 20);
}

class _Detail extends StatelessWidget {
  final Complaint complaint;
  const _Detail({required this.complaint});

  @override
  Widget build(BuildContext context) {
    final c = complaint;
    Widget row(String k, String v) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            SizedBox(width: 120, child: Text(k, style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant))),
            Expanded(child: Text(v, style: const TextStyle(fontWeight: FontWeight.w600))),
          ]),
        );
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Report #${c.id}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 10),
            if (c.photoUrl.isNotEmpty)
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(c.photoUrl, height: 160, width: double.infinity, fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => const SizedBox(height: 60, child: Center(child: Text('Photo not available')))),
              ),
            const SizedBox(height: 10),
            row('Status', c.statusLabel),
            row('Severity', '${c.severity} of 5'),
            row('Garbage type', c.garbageType),
            row('Coverage', '${c.coveragePct.round()}% of the frame'),
            row('Cleanup target', c.slaLabel),
            row('Location', '${c.latitude.toStringAsFixed(5)}, ${c.longitude.toStringAsFixed(5)}'),
            if (c.status == 'REOPENED')
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: Text('This spot was reported again after a cleanup, so the ticket was reopened and prioritised.'),
              ),
            if (c.status == 'CLEANED')
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: Text('Verified cleaned by a worker on site. 🪙 Clean Coins have been added to your balance.'),
              ),
          ],
        ),
      ),
    );
  }
}
