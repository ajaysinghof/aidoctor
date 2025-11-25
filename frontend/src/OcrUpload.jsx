import React, { useState } from "react";

export default function OcrUpload() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const uploadPdf = async () => {
    if (!file) {
      alert("Please select a PDF file.");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch("/api/ocr/extract", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }

      const data = await response.json(); // Backend returns JSON
      setResult(data);

    } catch (err) {
      console.error("OCR Error:", err);
      alert("❌ OCR Error: " + err.message);
    }

    setLoading(false);
  };

  return (
    <div className="ocr-box">
      <h2>📄 Upload Medical Report (PDF)</h2>

      <div className="dropzone">
        <input
          type="file"
          accept="application/pdf"
          onChange={(e) => setFile(e.target.files[0])}
        />
      </div>

      <button onClick={uploadPdf} disabled={loading}>
        {loading ? "Extracting..." : "Upload & Extract OCR"}
      </button>

      {result && (
        <div className="ocr-result">
          <h3>Extracted Text</h3>
          <pre>{result.text}</pre>

          <h3>Fields</h3>
          <pre>{JSON.stringify(result.fields, null, 2)}</pre>

          <p>Pages: {result.pages}</p>
        </div>
      )}
    </div>
  );
}
