import { initializeApp } from "firebase/app";
import {
  getAuth,
  onAuthStateChanged,
  signOut,
} from "firebase/auth";
import { getFirestore, collection, getDocs, doc, getDoc } from "firebase/firestore";

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

// Initialize dashboard
async function initDashboard() {
  onAuthStateChanged(auth, async (user) => {
    if (!user) {
      window.location.href = "popup.html";
      return;
    }
    
    await loadStats();
  });
  
  // Event listeners
  document.getElementById("refresh-btn").addEventListener("click", loadStats);
  document.getElementById("logout-btn").addEventListener("click", handleLogout);
}

async function loadStats() {
  const loading = document.getElementById("loading");
  const statsContainer = document.getElementById("app-list");
  const errorDiv = document.getElementById("error-message");
  
  loading.style.display = "block";
  statsContainer.innerHTML = "";
  errorDiv.style.display = "none";
  
  try {
    const user = auth.currentUser;
    if (!user) return;
    
    // Fetch per-app secondsToday from users/{uid}/apps
    const snap = await getDocs(collection(db, "users", user.uid, "apps"));
    const preferencesDoc = await getDoc(doc(db, "users", user.uid, "data", "preferences"));
    const DAILY_GOAL_MINUTES = preferencesDoc.data()?.dailyGoalMinutes || 240;
    let totalTimeSpent = 0; // minutes
    const appData = [];

    snap.forEach((docSnap) => {
      const appId = docSnap.id;
      const data = docSnap.data() || {};
      const seconds = data.secondsToday || 0;
      const spentMinutes = Math.round(seconds / 60);
      totalTimeSpent += spentMinutes;
      appData.push({ name: appId, spent: spentMinutes });
    });

    // Sort apps by most used
    appData.sort((a, b) => b.spent - a.spent);

    const hours = Math.floor(totalTimeSpent / 60);
    const minutes = totalTimeSpent % 60;

    // Ring progress vs goal
    const pct = Math.min(120, Math.round((totalTimeSpent / DAILY_GOAL_MINUTES) * 100));
    const ring = document.getElementById("progress-ring");
    ring?.style.setProperty("--pct", `${pct}`);

    document.getElementById("total-time").textContent = `${hours}h ${minutes}m`;
    const goalHours = Math.floor(DAILY_GOAL_MINUTES / 60);
    const goalMins = DAILY_GOAL_MINUTES % 60;
    document.getElementById("goal-text").textContent = `of ${goalHours}h ${goalMins}m goal`;

    const overGoal = totalTimeSpent - DAILY_GOAL_MINUTES;
    const overHours = Math.floor(Math.abs(overGoal) / 60);
    const overMins = Math.abs(overGoal) % 60;
    const overText = `${overGoal >= 0 ? "-" : ""}${overHours}h ${overMins}m`;
    document.getElementById("over-goal").textContent = overText;
    const appsUsedToday = appData.filter(app => app.spent > 0).length;
    document.getElementById("apps-used").textContent = appsUsedToday.toString();

    statsContainer.innerHTML = appData.map(app => `
      <div class="app-card">
        <div class="app-avatar">
          <img src="./icons/${app.name}.svg" alt="${app.name}" onerror="this.style.display='none';this.nextElementSibling.style.display='grid';">
          <span class="app-avatar-fallback" style="display:none;">${app.name.charAt(0).toUpperCase()}</span>
        </div>
        <div class="app-info">
          <div class="app-row">
            <div class="app-name">${app.name}</div>
            <div class="app-time">${app.spent}m</div>
          </div>
          <div class="app-bar">
            <div class="app-bar-fill" style="width: ${Math.min(app.spent, 240) / 240 * 100}%"></div>
          </div>
        </div>
      </div>
    `).join("");
    
    loading.style.display = "none";
  } catch (error) {
    console.error("Error loading stats:", error);
    errorDiv.textContent = "Failed to load stats: " + error.message;
    errorDiv.style.display = "block";
    loading.style.display = "none";
  }
}

async function handleLogout() {
  try {
    await signOut(auth);
    window.location.href = "popup.html";
    console.log("LINE 128 ERROR");
  } catch (error) {
    console.error("Logout error:", error);
  }
}

// Start dashboard
initDashboard();
