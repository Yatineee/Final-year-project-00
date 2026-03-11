import { initializeApp } from "firebase/app";
import { getAuth, onAuthStateChanged } from "firebase/auth";
import { getFirestore, doc, setDoc, serverTimestamp, increment, collection, getDocs, getDoc } from "firebase/firestore";

const firebaseConfig = {   
  apiKey: "AIzaSyAC-LLW7eNFKjbnHl0YMdxl_Y6xvRHXTes",
  authDomain: "arete-67667.firebaseapp.com",
  projectId: "arete-67667",
  storageBucket: "arete-67667.firebasestorage.app",
  messagingSenderId: "538378717816",
  appId: "1:538378717816:web:c14690b261eafd85647b8d",
  measurementId: "G-5N6KFPNF0K" 
};
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

let currentUser = null;
onAuthStateChanged(auth, (user) => {
  currentUser = user;
  console.log("[bg] auth state", user ? user.uid : "none");
});

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg?.type === "USAGE_DELTA") {
    const { appId, deltaSeconds, date } = msg;
    if (!appId || !deltaSeconds) {
      sendResponse?.({ ok: false, reason: "missing-data" });
      return true;
    }
    if (!currentUser) {
      console.warn("[bg] drop delta, no auth user", { appId, deltaSeconds });
      sendResponse?.({ ok: false, reason: "no-user" });
      return true;
    }
    const ref = doc(db, "users", currentUser.uid, "apps", appId);
    setDoc(ref, {
      date: date || new Date().toISOString().slice(0, 10),
      secondsToday: increment(deltaSeconds),
      lastUpdated: serverTimestamp(),
    }, { merge: true })
      .then(() => sendResponse?.({ ok: true }))
      .catch((err) => {
        console.error("[bg] USAGE_DELTA write failed", err);
        sendResponse?.({ ok: false, reason: err?.message || "write-failed" });
      });
    return true; // keep channel alive for async sendResponse
  }

  if (msg?.type === "GET_USER_ID") {
    sendResponse?.(currentUser?.uid || null);
    return false;
  }

  if (msg?.type === "GET_STATS") {
    const { userId } = msg;
    if (!userId) {
      sendResponse?.({ totalTimeSpent: 0, dailyGoalMinutes: 240 });
      return false;
    }

    (async () => {
      try {
        // Fetch all apps and sum up secondsToday
        const snap = await getDocs(collection(db, "users", userId, "apps"));
        let totalTimeSpent = 0;
        snap.forEach((docSnap) => {
          const data = docSnap.data() || {};
          const seconds = data.secondsToday || 0;
          totalTimeSpent += Math.round(seconds / 60); // convert to minutes
        });

        // Fetch daily goal from preferences
        const preferencesDoc = await getDoc(doc(db, "users", userId, "data", "preferences"));
        const dailyGoalMinutes = preferencesDoc.data()?.dailyGoalMinutes || 240;

        sendResponse?.({ totalTimeSpent, dailyGoalMinutes });
      } catch (error) {
        console.error("[bg] Error getting stats:", error);
        sendResponse?.({ totalTimeSpent: 0, dailyGoalMinutes: 240 });
      }
    })();

    return true; // keep channel alive for async sendResponse
  }

  if (msg?.type === "GET_EXTENSION_URL") {
    sendResponse?.(chrome.runtime.getURL("arete.html"));
    return false;
  }

  if (msg?.type === "REDIRECT_TO_DASHBOARD") {
    const areteUrl = "https://arete-67667.web.app/arete.html";
    chrome.tabs.update(sender.tab.id, { url: areteUrl });
    sendResponse?.({ ok: true });
    return false;
  }

  return false;
});

console.log("[bg] service worker loaded");