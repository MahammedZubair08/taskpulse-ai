import { useState } from "react";
import { register } from "../services/api";
function Register({ onSwitchToLogin }) {
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [loading, setLoading] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");
        setLoading(true);
        try {
            await register(
                firstName,
                lastName,
                email,
                password
            );
            setSuccess(
                "Registration successful. You can now login."
            );
            setFirstName("");
            setLastName("");
            setEmail("");
            setPassword("");
        } catch (err) {
            console.error(err);
            const data = err.response?.data;
            if (data?.data) {
                const validationErrors = Object.values(
                    data.data
                ).join(" ");
                setError(validationErrors);
            } else {
                setError(
                    data?.message ||
                    "Registration failed."
                );
            }
        } finally {
            setLoading(false);
        }
    };
    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>TaskPulse</h1>
                <h2>Create Account</h2>
                <form onSubmit={handleSubmit}>
                    <input
                        type="text"
                        placeholder="First Name"
                        value={firstName}
                        onChange={(e) =>
                            setFirstName(e.target.value)
                        }
                        required
                    />
                    <input
                        type="text"
                        placeholder="Last Name"
                        value={lastName}
                        onChange={(e) =>
                            setLastName(e.target.value)
                        }
                        required
                    />
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
                            ? "Creating..."
                            : "Register"}
                    </button>
                </form>
                {error && (
                    <p className="error">
                        {error}
                    </p>
                )}
                {success && (
                    <p className="success">
                        {success}
                    </p>
                )}
                <p className="auth-switch">
                    Already have an account?
                    <button
                        type="button"
                        onClick={onSwitchToLogin}
                    >
                        Login
                    </button>
                </p>
            </div>
        </div>
    );
}
export default Register;
