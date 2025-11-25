import React, { useState, useEffect } from "react";
import "./styles.css";

const API = (path) => `${process.env.REACT_APP_API_URL || "http://localhost:8080"}${path}`;

export default function App() {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [username, setUsername] = useState("");
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [loginState, setLoginState] = useState({ username: "", password: "" });
  const [registerState, setRegisterState] = useState({ username: "", password: "" });

  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);

  // Chatbox state
  const [messages, setMessages] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);

  useEffect(() => {
    if (token) loadHistory();
  }, [token]);

  async function login() {
    const res = await fetch(API("/api/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(loginState),
    });
    const j = await res.json();
    if (j.token) {
      setToken(j.token);
      localStorage.setItem("token", j.token);
      setUsername(j.username);
    } else alert(JSON.stringify(j));
  }

  async function register() {
    const res = await fetch(API("/api/auth/register"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(registerState),
    });
    const j = await res.json();
    if (j.token) {
      setToken(j.token);
      localStorage.setItem("token", j.token);
      setUsername(j.username);
    } else alert(JSON.stringify(j));
  }

  async function upload() {
    if (!file) return alert("Choose file");

    setUploading(true);
    setUploadProgress(0);

    const fd = new FormData();
    fd.append("file", file);

    const res = await fetch(API("/api/ocr/upload"), {
      method: "POST",
      headers: token ? { Authorization: "Bearer " + token } : {},
      body: fd,
    });

    const j = await res.json();
    if (res.ok) {
      setResult(j);
      loadHistory();
    } else alert(JSON.stringify(j));

    setUploading(false);
    setUploadProgress(100);
  }

  async function loadHistory() {
    if (!token) return;
    const res = await fetch(API("/api/ocr/history"), {
      headers: { Authorization: "Bearer " + token },
    });
    const j = await res.json();
    if (res.ok) setHistory(j);
  }

  function logout() {
    setToken("");
    localStorage.removeItem("token");
    setUsername("");
    setHistory([]);
    setResult(null);
  }

  // CHAT MESSAGE SEND
  async function sendChat() {
    if (!chatInput.trim()) return;

    const newMsg = { role: "user", text: chatInput };
    setMessages((m) => [...m, newMsg]);
    setChatInput("");
    setChatLoading(true);

    const res = await fetch(API("/api/chat/message"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: "Bearer " + token }),
      },
      body: JSON.stringify({ message: newMsg.text }),
    });

    const j = await res.json();

    setMessages((m) => [...m, { role: "bot", text: j.reply || "(no reply)" }]);
    setChatLoading(false);
  }

  return (
    <div className="container">
      <h1 className="title">AI Doctor — Modern OCR + Chat</h1>

      {!token ? (
        <div className="auth-box">
          <div className="auth-card">
            <h3>Login</h3>
            <input placeholder="username" value={loginState.username} onChange={(e) => setLoginState((s) => ({ ...s, username: e.target.value }))} />
            <input type="password" placeholder="password" value={loginState.password} onChange={(e) => setLoginState((s) => ({ ...s, password: e.target.value }))} />
            <button onClick={login}>Login</button>
          </div>

          <div className="auth-card">
            <h3>Register</h3>
            <input placeholder="username" value={registerState.username} onChange={(e) => setRegisterState((s) => ({ ...s, username: e.target.value }))} />
            <input type="password" placeholder="password" value={registerState.password} onChange={(e) => setRegisterState((s) => ({ ...s, password: e.target.value }))} />
            <button onClick={register}>Register</button>
          </div>
        </div>
      ) : (
        <div>
          <div className="top-bar">
            <span>Logged in as <b>{username}</b></span>
            <button onClick={logout}>Logout</button>
          </div>

          <div className="upload-section">
            <h3>Upload Medical Report</h3>
            <input type="file" accept=".pdf,image/*" onChange={(e) => setFile(e.target.files[0])} />
            <button onClick={upload}>Upload & Process</button>

            {uploading && (
              <div className="progress-bar">
                <div className="progress" style={{ width: `${uploadProgress}%` }}></div>
              </div>
            )}
          </div>

          {result && (
            <div className="result-box">
              <h3>OCR Result</h3>
              <div><b>Summary:</b> {result.summary}</div>
            </div>
          )}

          <h3>Chat with AI Doctor</h3>
          <div className="chat-box">
            {messages.map((m, i) => (
              <div key={i} className={m.role === "user" ? "chat-user" : "chat-bot"}>{m.text}</div>
            ))}
            {chatLoading && <div className="chat-bot">Typing...</div>}
          </div>

          <div className="chat-input">
            <input value={chatInput} onChange={(e) => setChatInput(e.target.value)} placeholder="Ask a medical question..." />
            <button onClick={sendChat}>Send</button>
          </div>

          <h3>History</h3>
          {history.map((r) => (
            <div key={r.id} className="history-item">
              <b>{r.originalFileName}</b> — {new Date(r.createdAt).toLocaleString()}
              <div>{r.summary}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
