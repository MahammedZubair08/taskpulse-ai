import { useState } from "react";
import { login } from "../services/api";
function Login({ onLogin, onSwitchToRegister }) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);
        try {
            const response = await login(
                email,
                password
            );
            console.log("Login response:", response);
            const token =
                response?.data?.token ||
                response?.data?.accessToken ||
                response?.data?.jwt ||
                response?.token ||
                response?.accessToken ||
                response?.jwt;
            if (!token) {
                console.error(
                    "Login response does not contain JWT:",
                    response
                );
                throw new Error(
                    "No JWT token received."
                );
            }
            localStorage.setItem("token", token);
            onLogin();
        } catch (err) {
            console.error("Login error:", err);
            setError(
                err.response?.data?.message ||
                err.message ||
                "Login failed."
            );
        } finally {
            setLoading(false);
        }
    };
    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>TaskPulse</h1>
                <h2>Login</h2>
                <form onSubmit={handleSubmit}>
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) =>
                            setEmail(e.target.value)
                        }
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) =>
                            setPassword(e.target.value)
                        }
                        required
                    />
                    <button
                        type="submit"
                        disabled={loading}
                    >
                        {loading
                            ? "Logging in..."
                            : "Login"}
                    </button>
                </form>
                {error && (
                    <p className="error">
                        {error}
                    </p>
                )}
                <p className="auth-switch">
                    Don't have an account?
                    <button
                        type="button"
                        onClick={onSwitchToRegister}
                    >
                        Register
                    </button>
                </p>
            </div>
        </div>
    );
}
export default Login;
