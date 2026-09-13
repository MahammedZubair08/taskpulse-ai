import { useState } from "react";
import Login from "./components/Login";
import Register from "./components/Register";
import Dashboard from "./pages/Dashboard";
import "./App.css";
function App() {
    const [authenticated, setAuthenticated] = useState(
        !!localStorage.getItem("token")
    );
    const [showRegister, setShowRegister] = useState(false);
    const logout = () => {
        localStorage.removeItem("token");
        setAuthenticated(false);
    };
    if (!authenticated) {
        if (showRegister) {
            return (
                <Register
                    onSwitchToLogin={() =>
                        setShowRegister(false)
                    }
                />
            );
        }
        return (
            <Login
                onLogin={() =>
                    setAuthenticated(true)
                }
                onSwitchToRegister={() =>
                    setShowRegister(true)
                }
            />
        );
    }
    return (
        <Dashboard onLogout={logout} />
    );
}
export default App;
