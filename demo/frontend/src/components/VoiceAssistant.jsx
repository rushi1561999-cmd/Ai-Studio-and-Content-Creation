import React, { useState, useEffect, useRef } from "react";
import "./VoiceAssistant.css";

const LANGUAGES = [
  { code: "en-US", name: "English (US)" },
  { code: "en-GB", name: "English (UK)" },
  { code: "es-ES", name: "Spanish" },
  { code: "fr-FR", name: "French" },
  { code: "de-DE", name: "German" },
  { code: "it-IT", name: "Italian" },
  { code: "pt-BR", name: "Portuguese (Brazil)" },
  { code: "zh-CN", name: "Chinese (Simplified)" },
  { code: "ja-JP", name: "Japanese" },
  { code: "ko-KR", name: "Korean" },
  { code: "hi-IN", name: "Hindi" },
  { code: "ar-SA", name: "Arabic" },
];

export default function VoiceAssistant({ onTranscript, onSpeak, disabled }) {
  const [isListening, setIsListening] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState("en-US");
  const [showLanguageMenu, setShowLanguageMenu] = useState(false);
  const [supportedLanguages, setSupportedLanguages] = useState([]);
  const recognitionRef = useRef(null);
  const synthesisRef = useRef(null);

  useEffect(() => {
    // Detect supported languages
    const detectSupportedLanguages = () => {
      const supported = [];
      
      // Check speech recognition support
      if ("webkitSpeechRecognition" in window || "SpeechRecognition" in window) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        const tempRecognition = new SpeechRecognition();
        
        LANGUAGES.forEach(lang => {
          try {
            tempRecognition.lang = lang.code;
            supported.push(lang);
          } catch (e) {
            console.warn(`Language ${lang.code} not supported for speech recognition`);
          }
        });
      } else {
        // If speech recognition not supported, still show languages for synthesis
        LANGUAGES.forEach(lang => supported.push(lang));
      }
      
      setSupportedLanguages(supported);
    };

    detectSupportedLanguages();

    // Initialize Speech Recognition
    if ("webkitSpeechRecognition" in window || "SpeechRecognition" in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      recognitionRef.current = new SpeechRecognition();
      recognitionRef.current.continuous = false;
      recognitionRef.current.interimResults = false;
      recognitionRef.current.lang = selectedLanguage;

      recognitionRef.current.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        if (onTranscript) {
          onTranscript(transcript);
        }
        setIsListening(false);
      };

      recognitionRef.current.onerror = (event) => {
        console.error("Speech recognition error:", event.error);
        setIsListening(false);
      };

      recognitionRef.current.onend = () => {
        setIsListening(false);
      };
    }

    // Initialize Speech Synthesis
    if ("speechSynthesis" in window) {
      synthesisRef.current = window.speechSynthesis;
    }

    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      if (synthesisRef.current) {
        synthesisRef.current.cancel();
      }
    };
  }, [selectedLanguage, onTranscript]);

  useEffect(() => {
    if (recognitionRef.current) {
      recognitionRef.current.lang = selectedLanguage;
    }
  }, [selectedLanguage]);

  const startListening = () => {
    if (!recognitionRef.current) {
      alert("Speech recognition is not supported in your browser.");
      return;
    }

    if (isSpeaking) {
      synthesisRef.current.cancel();
      setIsSpeaking(false);
    }

    try {
      recognitionRef.current.start();
      setIsListening(true);
    } catch (error) {
      console.error("Error starting speech recognition:", error);
      setIsListening(false);
    }
  };

  const stopListening = () => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
      setIsListening(false);
    }
  };

  const speak = (text) => {
    if (!synthesisRef.current) {
      alert("Speech synthesis is not supported in your browser.");
      return;
    }

    if (isListening) {
      stopListening();
    }

    synthesisRef.current.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = selectedLanguage;
    utterance.rate = 1;
    utterance.pitch = 1;

    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);

    synthesisRef.current.speak(utterance);
  };

  const handleSpeakClick = () => {
    if (onSpeak) {
      onSpeak();
    }
  };

  return (
    <div className="voice-assistant">
      <div className="voice-controls">
        <button
          type="button"
          className={`voice-btn ${isListening ? "listening" : ""}`}
          onClick={isListening ? stopListening : startListening}
          disabled={disabled}
          title={isListening ? "Stop listening" : "Start voice input"}
        >
          <span className="voice-icon">🎤</span>
          {isListening && <span className="voice-indicator" />}
        </button>

        <button
          type="button"
          className={`voice-btn ${isSpeaking ? "speaking" : ""}`}
          onClick={handleSpeakClick}
          disabled={disabled}
          title="Read aloud"
        >
          <span className="voice-icon">🔊</span>
        </button>

        <div className="language-selector">
          <button
            type="button"
            className="language-btn"
            onClick={() => setShowLanguageMenu(!showLanguageMenu)}
            disabled={disabled}
            title="Select language"
          >
            🌐
          </button>

          {showLanguageMenu && (
            <div className="language-menu">
              {supportedLanguages.map((lang) => (
                <button
                  key={lang.code}
                  type="button"
                  className={`language-option ${selectedLanguage === lang.code ? "selected" : ""}`}
                  onClick={() => {
                    setSelectedLanguage(lang.code);
                    setShowLanguageMenu(false);
                  }}
                >
                  {lang.name}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {isListening && (
        <div className="voice-status listening">
          <span className="status-dot" />
          Listening... Speak now
        </div>
      )}

      {isSpeaking && (
        <div className="voice-status speaking">
          <span className="status-dot" />
          Speaking...
        </div>
      )}
    </div>
  );
}
