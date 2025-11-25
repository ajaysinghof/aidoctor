import React, { useState, useEffect, useRef } from "react";
import "./styles.css";

const API = (path) => `${process.env.REACT_APP_API_URL || "http://localhost:8080"}${path}`;

export default function App() {
  const [token] = useState(localStorage.getItem("token") || "");
  const [username] = useState("Ajay");   // Show name dynamically if needed
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [ocrResult, setOcrResult] = useState(null);
  const [chatMode, setChatMode] = useState("none");  
  // none → initial
  // chat → user wants chat
  // upload → user wants upload flow

  const [messages, setMessages] = useState([
    { role: "bot", text: "Hello 👋 — I’m Dr. Raghav. What would you like to do today?", time: Date.now() }
  ]);

  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const chatEndRef = useRef(null);

  useEffect(() => {
    if (chatEndRef.current) chatEndRef.current.scrollIntoView({ behavior: "smooth" });
  }, [messages, chatLoading]);

  // ------------------------
  // HANDLE FIRST MESSAGE
  // ------------------------
  const handleFirstMessage = (text) => {
    setMessages((m) => [
      ...m,
      { role: "user", text, time: Date.now() },
      {
        role: "bot",
        text: `Hi ${username} 👋\nHow may I assist you today?`,
        time: Date.now()
      }
    ]);

    // Offer options
    setTimeout(() => {
      setMessages((m) => [
        ...m,
        {
          role: "bot",
          text:
            "Would you like to:\n\n1️⃣ Chat with Doctor\n2️⃣ Upload Medical Report?",
          showOptions: true,
          time: Date.now()
        }
      ]);
    }, 700);
  };

  // -----------------------------
  // OPTION SELECTION
  // -----------------------------
  const chooseChat = () => {
    setChatMode("chat");
    setMessages((m) => [
      ...m,
      {
        role: "bot",
        text: "Great 👨‍⚕️ — What symptoms are you experiencing today?",
        time: Date.now()
      }
    ]);
  };

  const chooseUpload = () => {
    setChatMode("upload");
    setMessages((m) => [
      ...m,
      {
        role: "bot",
        text: "Please upload your medical report (PDF or Image) 📄",
        time: Date.now()
      }
    ]);
  };

  // -----------------------------
  // FILE UPLOAD + OCR → SUMMARY
  // -----------------------------
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
        setProgress(Math.round((e.loaded / e.total) * 100));
      }
    };

    xhr.onreadystatechange = () => {
      if (xhr.readyState === 4) {
        setUploading(false);
        setProgress(100);

        // SUCCESS
        if (xhr.status >= 200 && xhr.status < 300) {
          const parsed = JSON.parse(xhr.responseText);
          setOcrResult(parsed);

          setMessages((m) => [
            ...m,
            {
              role: "bot",
              text:
                "I’ve reviewed your report. Here is my medical interpretation:",
              time: Date.now()
            },
            {
              role: "bot",
              text: parsed.summary || "No summary available.",
              time: Date.now()
            }
          ]);

          return;
        }

        // FAILURE
        let message = `OCR failed (${xhr.status})`;
        try {
          const json = JSON.parse(xhr.responseText);
          message = json.message || JSON.stringify(json);
        } catch {
          message = xhr.responseText || message;
        }

        setMessages((m) => [
          ...m,
          { role: "bot", text: `❌ OCR Error: ${message}`, time: Date.now() }
        ]);
      }
    };

    xhr.send(form);
  };

  // -----------------------------
  // NORMAL CHAT MODE
  // -----------------------------
  const sendChat = async () => {
    const text = chatInput.trim();
    if (!text) return;

    // FIRST MESSAGE → INIT FLOW
    if (messages.length === 1 && chatMode === "none") {
      setChatInput("");
      return handleFirstMessage(text);
    }

    if (chatMode !== "chat") {
      setMessages((m) => [
        ...m,
        {
          role: "bot",
          text: "Please choose an option: Chat or Upload report.",
          time: Date.now()
        }
      ]);
      return;
    }

    setMessages((m) => [...m, { role: "user", text, time: Date.now() }]);
    setChatInput("");
    setChatLoading(true);

    try {
      const res = await fetch(API("/api/chat/message"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: username, text })
      });

      let reply;
      try {
        const j = await res.json();
        reply = j.reply || j.text || JSON.stringify(j);
      } catch {
        reply = await res.text();
      }

      setMessages((m) => [...m, { role: "bot", text: reply, time: Date.now() }]);
    } catch {
      setMessages((m) => [
        ...m,
        { role: "bot", text: "❌ Error contacting server.", time: Date.now() }
      ]);
    } finally {
      setChatLoading(false);
    }
  };

  return (
    <div className="wrap">
      {/* --- HEADER --- */}
      <header className="header">
        <div className="brand">
          <div className="logo">AI Doctor</div>
          <div className="subtitle">Clinic Portal</div>
        </div>
        <div className="doctor-profile">
          <img className="doctor-avatar" src="/doctor-male.png" alt="doc" />
          <div className="doctor-meta">
            <div className="doc-name">Dr. Raghav</div>
            <div className="doc-role">General Physician</div>
          </div>
        </div>
      </header>

      {/* --- MAIN GRID --- */}
      <main className="main-grid">
        {/* LEFT COLUMN — upload box shown only if upload flow */}
        <section className="left">
          {chatMode === "upload" && (
            <div className="card">
              <h3>Upload Medical Report</h3>
              <div className="upload-row">
                <label className="file-chooser">
                  <input
                    type="file"
                    accept=".pdf,image/*"
                    onChange={(e) => setFile(e.target.files?.[0] || null)}
                  />
                  {file ? file.name : "Choose file"}
                </label>
                <button className="btn primary" onClick={uploadFile}>
                  Upload & Analyze
                </button>
              </div>

              {uploading && (
                <div className="progress-wrap">
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        {/* RIGHT: CHAT */}
        <aside className="right">
          <div className="card chat-card">
            <div className="chat-header">
              <div className="chat-title">Consultation</div>
              <div className="chat-sub">Chat with Dr. Raghav</div>
            </div>

            <div className="chat-body">
              {messages.map((m, i) => (
                <div key={i} className={`chat-message ${m.role}`}>
                  {m.role === "bot" && (
                    <img className="avatar" src="/doctor-male.png" alt="doc" />
                  )}
                  <div className="bubble">
                    <div className="bubble-text">
                      {m.text}

                      {/* Option Buttons */}
                      {m.showOptions && (
                        <div className="option-buttons">
                          <button onClick={chooseChat} className="btn small">
                            💬 Chat with Doctor
                          </button>
                          <button onClick={chooseUpload} className="btn small">
                            📄 Upload Report
                          </button>
                        </div>
                      )}
                    </div>
                    <div className="bubble-time">
                      {new Date(m.time).toLocaleTimeString()}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={chatEndRef} />
            </div>

            {/* CHAT INPUT */}
            <div className="chat-input-row">
              <input
                className="chat-input"
                placeholder="Type your message..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && sendChat()}
              />
              <button className="btn" onClick={sendChat}>
                Send
              </button>
            </div>
          </div>
        </aside>
      </main>

      <footer className="footer">
        © {new Date().getFullYear()} AI Doctor
      </footer>
    </div>
  );
}
