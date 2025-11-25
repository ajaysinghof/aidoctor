import React, { useState, useEffect, useRef } from "react";
import "./styles.css";

const API = (path) => `${process.env.REACT_APP_API_URL || "http://localhost:8080"}${path}`;

export default function App() {
  const [token] = useState(localStorage.getItem("token") || "");
  const [username] = useState("Patient");
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [ocrResult, setOcrResult] = useState(null);
  const [messages, setMessages] = useState([
    { role: "bot", text: "Hi — I'm Dr. Raghav. Describe your symptoms or upload a report.", time: Date.now() }
  ]);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const chatEndRef = useRef(null);

  useEffect(() => {
    if (chatEndRef.current) chatEndRef.current.scrollIntoView({ behavior: "smooth" });
  }, [messages, chatLoading]);

  // Upload with progress (XHR to get progress events)
  const uploadFile = () => {
    if (!file) return alert("Please select a file (PDF / JPG / PNG).");

    setUploading(true);
    setProgress(0);
    setOcrResult(null);

    const form = new FormData();
    form.append("file", file);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", API("/api/ocr/extract"), true);
    if (token) xhr.setRequestHeader("Authorization", "Bearer " + token);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const pct = Math.round((e.loaded / e.total) * 100);
        setProgress(pct);
      }
    };

    xhr.onreadystatechange = () => {
      if (xhr.readyState === 4) {
        setUploading(false);
        setProgress(100);
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const parsed = JSON.parse(xhr.responseText);
            setOcrResult(parsed);
            // push assistant summary to chat
            setMessages((m) => [
              ...m,
              { role: "bot", text: parsed.summary || "Report processed. See details.", time: Date.now() }
            ]);
          } catch (err) {
            // response was probably plain text — keep it safe
            setOcrResult({ text: xhr.responseText });
            setMessages((m) => [...m, { role: "bot", text: xhr.responseText, time: Date.now() }]);
          }
        } else {
          // server error
          let message = `OCR failed (${xhr.status})`;
          try {
            const json = JSON.parse(xhr.responseText);
            message = json.message || JSON.stringify(json);
          } catch (e) {
            message = xhr.responseText || message;
          }
          setMessages((m) => [...m, { role: "bot", text: `❌ OCR Error: ${message}`, time: Date.now() }]);
        }
      }
    };

    xhr.send(form);
  };

  // Simple chat send (POST to /api/chat/message) — expects JSON { reply: "..."}
  const sendChat = async () => {
    const text = chatInput.trim();
    if (!text) return;
    setMessages((m) => [...m, { role: "user", text, time: Date.now() }]);
    setChatInput("");
    setChatLoading(true);

    try {
      const res = await fetch(API("/api/chat/message"), {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(token && { Authorization: "Bearer " + token }) },
        body: JSON.stringify({ userId: "patient", text })
      });

      // prefer JSON reply, fallback to text
      let reply;
      try {
        const j = await res.json();
        reply = j.reply || j.text || JSON.stringify(j);
      } catch {
        reply = await res.text();
      }

      setMessages((m) => [...m, { role: "bot", text: reply, time: Date.now() }]);
    } catch (err) {
      setMessages((m) => [...m, { role: "bot", text: "❌ Error contacting server. Try again.", time: Date.now() }]);
    } finally {
      setChatLoading(false);
    }
  };

  return (
    <div className="wrap">
      {/* Header */}
      <header className="header">
        <div className="brand">
          <div className="logo">AI Doctor</div>
          <div className="subtitle">Clinic Portal</div>
        </div>

        <div className="doctor-profile">
          <img className="doctor-avatar" src="/doctor-male.png" alt="Dr avatar" />
          <div className="doctor-meta">
            <div className="doc-name">Dr. Raghav</div>
            <div className="doc-role">General Physician</div>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className="main-grid">
        {/* Left column: upload + dashboard */}
        <section className="left">
          <div className="card">
            <h3>Upload Medical Report</h3>
            <p className="muted">PDF or image. Textract + AI will analyze the contents.</p>

            <div className="upload-row">
              <label className="file-chooser">
                <input type="file" accept=".pdf,image/*" onChange={(e) => setFile(e.target.files?.[0] || null)} />
                {file ? file.name : "Choose a file"}
              </label>
              <button className="btn primary" onClick={uploadFile} disabled={uploading}>
                {uploading ? "Uploading..." : "Upload & Analyze"}
              </button>
            </div>

            {uploading && (
              <div className="progress-wrap">
                <div className="progress-bar">
                  <div className="progress-fill" style={{ width: `${progress}%` }} />
                </div>
                <div className="progress-text">{progress}%</div>
              </div>
            )}

            {ocrResult && (
              <div className="result-card">
                <h4>Extraction Summary</h4>
                <div className="summary">{ocrResult.summary || ocrResult.text || "No summary provided."}</div>
                {ocrResult.fields && (
                  <div className="fields-grid">
                    {Object.entries(ocrResult.fields).map(([k, v]) => (
                      <div key={k} className="field">
                        <div className="field-key">{k}</div>
                        <div className="field-val">{v}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="dashboard-cards">
            <div className="card small">
              <div className="stat">Active Patients</div>
              <div className="big">24</div>
            </div>
            <div className="card small">
              <div className="stat">Reports Today</div>
              <div className="big">6</div>
            </div>
            <div className="card small">
              <div className="stat">Alerts</div>
              <div className="big alert">2</div>
            </div>
          </div>
        </section>

        {/* Right column: chat */}
        <aside className="right">
          <div className="card chat-card">
            <div className="chat-header">
              <div className="chat-title">Consultation</div>
              <div className="chat-sub">Chat with Dr. Raghav</div>
            </div>

            <div className="chat-body">
              {messages.map((m, i) => (
                <div key={i} className={`chat-message ${m.role === "user" ? "user" : "bot"}`}>
                  {m.role === "bot" && <img className="avatar" src="/doctor-male.png" alt="doc" />}
                  <div className="bubble">
                    <div className="bubble-text">{m.text}</div>
                    <div className="bubble-time">{new Date(m.time).toLocaleTimeString()}</div>
                  </div>
                  {m.role === "user" && <img className="avatar user-avatar" src="/patient.png" alt="you" />}
                </div>
              ))}
              <div ref={chatEndRef} />
            </div>

            <div className="chat-input-row">
              <input
                className="chat-input"
                placeholder="Describe symptoms or ask about the report..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && sendChat()}
                disabled={chatLoading}
              />
              <button className="btn" onClick={sendChat} disabled={chatLoading}>
                {chatLoading ? "Thinking..." : "Send"}
              </button>
            </div>
          </div>

          <div className="card tips">
            <h4>Quick Tips</h4>
            <ul>
              <li>Upload lab reports (CBC, LFT, KFT) for structured extraction.</li>
              <li>Ask symptom-specific questions (onset, duration).</li>
              <li>For emergencies, call your doctor — AI is for guidance only.</li>
            </ul>
          </div>
        </aside>
      </main>

      <footer className="footer">
        <div>© {new Date().getFullYear()} AI Doctor — Clinic Portal</div>
      </footer>
    </div>
  );
}
