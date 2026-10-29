import 'package:flutter/material.dart';

import 'api.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Session.load();
  runApp(const SwachhApp());
}

class SwachhApp extends StatelessWidget {
  const SwachhApp({super.key});

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF0F7A4A);
    return MaterialApp(
      title: 'SwachhDrishti',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorSchemeSeed: seed, useMaterial3: true),
      darkTheme: ThemeData(colorSchemeSeed: seed, brightness: Brightness.dark, useMaterial3: true),
      home: const RootGate(),
    );
  }
}

/// Shows the login screen or the app depending on whether a token is stored.
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
        : LoginScreen(onLoggedIn: () => setState(() => _loggedIn = true));
  }
}
