// frontend/src/App.jsx
import React, { useState, useEffect } from "react";

const API = (path) =>
  `${process.env.REACT_APP_API_URL || "http://localhost:8080"}${path}`;

function App() {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [username, setUsername] = useState("");
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [isUploading, setIsUploading] = useState(false);

  // AI Chat states
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);

  const [loginState, setLoginState] = useState({ username: "", password: "" });
  const [registerState, setRegisterState] = useState({
    username: "",
    password: "",
  });

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
    const fd = new FormData();
    fd.append("file", file);

    setIsUploading(true);

    const res = await fetch(API("/api/ocr/upload"), {
      method: "POST",
      headers: token ? { Authorization: "Bearer " + token } : {},
      body: fd,
    });

    const j = await res.json();
    setIsUploading(false);

    if (res.ok) {
      setResult(j);
      loadHistory();
    } else alert(JSON.stringify(j));
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

  // ------------------------------------
  // 🔥 AI Medical Chat Function
  // ------------------------------------
  async function sendChat() {
    if (!chatInput.trim()) return;

    const userMsg = { role: "user", text: chatInput };
    setChatMessages((m) => [...m, userMsg]);
    setChatInput("");
    setChatLoading(true);

    const res = await fetch(API("/api/ai/chat"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: "Bearer " + token }),
      },
      body: JSON.stringify({ message: userMsg.text }),
    });

    const j = await res.json();
    setChatLoading(false);

    if (res.ok) {
      const botMsg = { role: "assistant", text: j.reply };
      setChatMessages((m) => [...m, botMsg]);
    } else {
      alert("Chat error: " + JSON.stringify(j));
    }
  }

  // -------------------------------------------------------------------
  // UI + Modern Design
  // -------------------------------------------------------------------
  return (
    <div
      style={{
        maxWidth: 1000,
        margin: "1rem auto",
        fontFamily: "Inter, sans-serif",
        padding: 20,
      }}
    >
      <h1 style={{ textAlign: "center", marginBottom: 30 }}>
        ⚕️ AI Doctor – Smart Medical Assistant
      </h1>

      {/* LOGIN / REGISTER */}
      {!token ? (
        <div
          style={{
            display: "flex",
            gap: 20,
            backdropFilter: "blur(10px)",
            padding: 20,
            borderRadius: 12,
            background: "rgba(255,255,255,0.2)",
            border: "1px solid #ddd",
          }}
        >
          <div style={{ flex: 1 }}>
            <h3>Login</h3>
            <input
              placeholder="username"
              value={loginState.username}
              onChange={(e) =>
                setLoginState((s) => ({ ...s, username: e.target.value }))
              }
            />
            <br />
            <input
              placeholder="password"
              type="password"
              value={loginState.password}
              onChange={(e) =>
                setLoginState((s) => ({ ...s, password: e.target.value }))
              }
            />
            <br />
            <button onClick={login}>Login</button>
          </div>

          <div style={{ flex: 1 }}>
            <h3>Register</h3>
            <input
              placeholder="username"
              value={registerState.username}
              onChange={(e) =>
                setRegisterState((s) => ({ ...s, username: e.target.value }))
              }
            />
            <br />
            <input
              placeholder="password"
              type="password"
              value={registerState.password}
              onChange={(e) =>
                setRegisterState((s) => ({ ...s, password: e.target.value }))
              }
            />
            <br />
            <button onClick={register}>Register</button>
          </div>
        </div>
      ) : (
        <>
          {/* Logged in block */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              marginBottom: 20,
            }}
          >
            <h3>Welcome, {username}</h3>
            <button onClick={logout}>Logout</button>
          </div>

          {/* ---------------- UPLOAD BOX ---------------- */}
          <div
            style={{
              padding: 20,
              borderRadius: 12,
              background: "#f8f8ff",
              border: "1px solid #e0e0e0",
              marginBottom: 30,
            }}
          >
            <h3>Upload Medical Report</h3>
            <input
              type="file"
              accept=".pdf,image/*"
              onChange={(e) => setFile(e.target.files[0])}
            />

            <button onClick={upload} style={{ marginLeft: 10 }}>
              Upload & Process
            </button>

            {/* Loader */}
            {isUploading && (
              <div
                style={{
                  marginTop: 15,
                  padding: 10,
                  background: "#eef",
                  borderRadius: 8,
                  fontWeight: "bold",
                }}
              >
                🔄 Reading Document… Please wait
                <div
                  style={{
                    height: 8,
                    marginTop: 10,
                    borderRadius: 10,
                    background: "#ddd",
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      height: "100%",
                      width: "80%",
                      animation: "loadingBar 1.5s infinite",
                      background: "#4a90e2",
                    }}
                  />
                </div>
              </div>
            )}
          </div>

          {/* --------------------------------- RESULT --------------------------------- */}
          {result && (
            <div
              style={{
                padding: 20,
                border: "1px solid #ddd",
                borderRadius: 10,
                background: "white",
                marginBottom: 40,
              }}
            >
              <h3>📄 Latest Result</h3>
              <p>
                <b>File:</b> {result.originalFileName}
              </p>
              <p>
                <b>Summary:</b> {result.summary}
              </p>

              <h4 style={{ marginTop: 20 }}>Test Results</h4>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr>
                    <th>Test</th>
                    <th>Value</th>
                    <th>Unit</th>
                    <th>Interpretation</th>
                  </tr>
                </thead>
                <tbody>
                  {result.testResults?.map((tr) => (
                    <tr key={tr.id}>
                      <td>{tr.name}</td>
                      <td>{tr.value}</td>
                      <td>{tr.unit}</td>
                      <td
                        style={{
                          color:
                            tr.interpretation === "low"
                              ? "orange"
                              : tr.interpretation === "high"
                              ? "red"
                              : "green",
                        }}
                      >
                        {tr.interpretation}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* --------------------------------- HISTORY --------------------------------- */}
          <h3>📚 Past Reports</h3>
          {history.map((r) => (
            <div
              key={r.id}
              style={{
                padding: 12,
                background: "#fafafa",
                borderRadius: 8,
                marginBottom: 10,
                border: "1px solid #eee",
              }}
            >
              <b>{r.originalFileName}</b> —{" "}
              {new Date(r.createdAt).toLocaleString()}
              <br />
              {r.summary}
            </div>
          ))}

          {/* --------------------------------- CHATBOT --------------------------------- */}
          <div
            style={{
              marginTop: 40,
              padding: 20,
              borderRadius: 12,
              background: "#f0f7ff",
              border: "1px solid #cbd7ef",
            }}
          >
            <h2>💬 AI Medical Assistant</h2>

            <div
              style={{
                height: 300,
                overflowY: "auto",
                background: "white",
                padding: 15,
                borderRadius: 10,
                border: "1px solid #ddd",
                marginBottom: 15,
              }}
            >
              {chatMessages.map((msg, i) => (
                <div
                  key={i}
                  style={{
                    marginBottom: 15,
                    textAlign: msg.role === "user" ? "right" : "left",
                  }}
                >
                  <div
                    style={{
                      display: "inline-block",
                      padding: "10px 14px",
                      borderRadius: 12,
                      background: msg.role === "user" ? "#4a90e2" : "#eee",
                      color: msg.role === "user" ? "white" : "black",
                      maxWidth: "70%",
                    }}
                  >
                    {msg.text}
                  </div>
                </div>
              ))}

              {chatLoading && (
                <div style={{ textAlign: "left", opacity: 0.6 }}>
                  ⏳ Doctor is typing…
                </div>
              )}
            </div>

            <div style={{ display: "flex", gap: 10 }}>
              <input
                style={{ flex: 1, padding: 10, borderRadius: 10 }}
                placeholder="Ask a medical question…"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
              />
              <button onClick={sendChat}>Send</button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default App;
