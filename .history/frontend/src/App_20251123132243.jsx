// frontend/src/App.jsx
import React, { useState, useEffect } from "react";

const API = (path) => `${process.env.REACT_APP_API_URL || "http://localhost:8080"}${path}`;

function App(){
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [username, setUsername] = useState("");
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [loginState, setLoginState] = useState({username:"",password:""});
  const [registerState, setRegisterState] = useState({username:"",password:""});

  useEffect(()=> { if(token) loadHistory(); }, [token]);

  async function login(){
    const res = await fetch(API("/api/auth/login"), {
      method:"POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify(loginState)
    });
    const j = await res.json();
    if(j.token){ setToken(j.token); localStorage.setItem("token", j.token); setUsername(j.username); }
    else alert(JSON.stringify(j));
  }

  async function register(){
    const res = await fetch(API("/api/auth/register"), {
      method:"POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify(registerState)
    });
    const j = await res.json();
    if(j.token){ setToken(j.token); localStorage.setItem("token", j.token); setUsername(j.username); }
    else alert(JSON.stringify(j));
  }

  async function upload(){
    if(!file) return alert("Choose file");
    const fd = new FormData();
    fd.append("file", file);
    const res = await fetch(API("/api/ocr/upload"), {
      method:"POST",
      headers: token ? { Authorization: "Bearer " + token } : {},
      body: fd
    });
    const j = await res.json();
    if(res.ok){ setResult(j); loadHistory(); }
    else alert(JSON.stringify(j));
  }

  async function loadHistory(){
    if(!token) return;
    const res = await fetch(API("/api/ocr/history"), {
      headers: { Authorization: "Bearer " + token }
    });
    const j = await res.json();
    if(res.ok) setHistory(j);
    else console.warn("history error", j);
  }

  function logout(){ setToken(""); localStorage.removeItem("token"); setUsername(""); setHistory([]); setResult(null); }

  return (
    <div style={{maxWidth:900, margin:"1rem auto", fontFamily:"Inter, Arial"}}>
      <h1>AI Doctor — OCR & Reports</h1>

      {!token ? (
        <div style={{display:"flex",gap:20}}>
          <div style={{flex:1}}>
            <h3>Login</h3>
            <input placeholder="username" value={loginState.username} onChange={e=>setLoginState(s=>({...s,username:e.target.value}))} /><br/>
            <input placeholder="password" type="password" value={loginState.password} onChange={e=>setLoginState(s=>({...s, password:e.target.value}))} /><br/>
            <button onClick={login}>Login</button>
          </div>
          <div style={{flex:1}}>
            <h3>Register</h3>
            <input placeholder="username" value={registerState.username} onChange={e=>setRegisterState(s=>({...s,username:e.target.value}))} /><br/>
            <input placeholder="password" type="password" value={registerState.password} onChange={e=>setRegisterState(s=>({...s, password:e.target.value}))} /><br/>
            <button onClick={register}>Register</button>
          </div>
        </div>
      ) : (
        <div>
          <div style={{display:"flex", justifyContent:"space-between", alignItems:"center"}}>
            <div>Logged in as <b>{username}</b></div>
            <div>
              <button onClick={logout}>Logout</button>
            </div>
          </div>

          <hr/>

          <h3>Upload report (PDF / JPG / PNG)</h3>
          <input type="file" accept=".pdf,image/*" onChange={e=>setFile(e.target.files[0])} />
          <button onClick={upload}>Upload & Process</button>

          {result && (
            <div style={{marginTop:20, padding:12, border:"1px solid #ddd", borderRadius:8}}>
              <h3>Latest Result</h3>
              <div><strong>File:</strong> {result.originalFileName}</div>
              <div><strong>Summary:</strong> {result.summary}</div>
              <h4>Test results</h4>
              <table style={{width:"100%", borderCollapse:"collapse"}}>
                <thead><tr><th style={{textAlign:"left"}}>Test</th><th>Value</th><th>Unit</th><th>Interpretation</th></tr></thead>
                <tbody>
                  {result.testResults && result.testResults.map(tr => (
                    <tr key={tr.id}>
                      <td>{tr.name}</td>
                      <td>{tr.value}</td>
                      <td>{tr.unit}</td>
                      <td style={{color: tr.interpretation==="low"?"orange": tr.interpretation==="high"?"red":"green"}}>{tr.interpretation}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <hr/>
          <h3>History</h3>
          {history.map(r => (
            <div key={r.id} style={{border:"1px solid #eee", padding:10, marginBottom:8}}>
              <div><b>{r.originalFileName}</b> — {new Date(r.createdAt).toLocaleString()}</div>
              <div>{r.summary}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default App;
