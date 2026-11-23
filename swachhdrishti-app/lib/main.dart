import 'package:flutter/material.dart';

import 'api.dart';
import 'screens/auth_screen.dart';
import 'screens/home_screen.dart';
import 'theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Session.load();
  runApp(const SwachhApp());
}

class SwachhApp extends StatelessWidget {
  const SwachhApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SwachhDrishti',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: ThemeMode.system,
      home: const RootGate(),
    );
  }
}

/// Shows the sign-in / sign-up screen or the app, depending on whether a token is stored.
class RootGate extends StatefulWidget {
  const RootGate({super.key});

  @override
  State<RootGate> createState() => _RootGateState();
}

class _RootGateState extends State<RootGate> {
  bool _loggedIn = Session.isLoggedIn;

  Future<void> _logout() async {
    await Session.clear();
    if (mounted) setState(() => _loggedIn = false);
  }

  @override
  Widget build(BuildContext context) {
    return _loggedIn
        ? HomeScreen(onLogout: _logout)
        : AuthScreen(onAuthenticated: () => setState(() => _loggedIn = true));
  }
}
