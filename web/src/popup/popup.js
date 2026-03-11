import { initializeApp } from "firebase/app";
import {
  getAuth,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
} from "firebase/auth";
import { getFirestore, doc, setDoc } from "firebase/firestore";

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

// Keep this at the top level to detect login state immediately
onAuthStateChanged(auth, (user) => {
  if (user) {
    console.log("User is logged in:", user.uid);
    showMessage(`Signed in as ${user.email || user.uid}`);
  } else {
    console.log("No user logged in.");
    showMessage("Signed out. Log in or create an account.");
  }
});

function showMessage(text) {
  const el = document.getElementById("auth-message");
  if (el) {
    el.textContent = text;
  }
}

async function handleSignIn(email, password) {
  const { user } = await signInWithEmailAndPassword(auth, email, password);
  return user;
}

async function handleSignUp(email, password) {
  const { user } = await createUserWithEmailAndPassword(auth, email, password);
  
  // Create user document in Firestore
  await setDoc(doc(db, "users", user.uid), {
    email: user.email,
    uid: user.uid
  });
  await initializeUserApps(user.uid);
  
  return user;
}

async function initializeUserApps(uid) {
  const today = new Date().toISOString().slice(0, 10);
  const apps = [
    "instagram",
    "facebook",
    "twitter",
    "reddit",
    "tiktok",
    "youtube",
    "snapchat",
  ];
  await Promise.all(apps.map((appId) => setDoc(
    doc(db, "users", uid, "apps", appId),
    { secondsToday: 0, date: today, lastUpdated: null },
    { merge: true }
  )));
}

function wireUi() {
  const form = document.getElementById("auth-form");
  const emailInput = document.getElementById("email");
  const passwordInput = document.getElementById("password");
  const loginBtn = document.getElementById("email-signin");
  const signupBtn = document.getElementById("email-signup");
  const toggleLink = document.getElementById("toggle-auth");
  const toggleText = document.getElementById("toggle-text");
  const authTitle = document.getElementById("auth-title");
  
  let isSignUpMode = false;

  // Toggle between sign-in and sign-up
  if (toggleLink) {
    toggleLink.addEventListener("click", (evt) => {
      evt.preventDefault();
      isSignUpMode = !isSignUpMode;
      
      if (isSignUpMode) {
        authTitle.textContent = "Sign Up";
        loginBtn.style.display = "none";
        signupBtn.style.display = "block";
        toggleText.textContent = "Already have an account? ";
        toggleLink.textContent = "Log in";
      } else {
        authTitle.textContent = "Log In";
        loginBtn.style.display = "block";
        signupBtn.style.display = "none";
        toggleText.textContent = "Don't have an account? ";
        toggleLink.textContent = "Sign up";
      }
      showMessage("");
    });
  }

  if (form) {
    form.addEventListener("submit", async (evt) => {
      evt.preventDefault();
      if (isSignUpMode) signupBtn?.click();
      else loginBtn?.click();
    });
  }

  if (loginBtn) {
    loginBtn.addEventListener("click", async () => {
      const email = emailInput?.value?.trim();
      const password = passwordInput?.value || "";
      if (!email || !password) {
        showMessage("Enter email and password.");
        return;
      }
      const originalText = loginBtn.textContent;
      loginBtn.disabled = true;
      loginBtn.textContent = "Signing in...";
      showMessage("Signing in...");
      try {
        await handleSignIn(email, password);
        showMessage("Signed in.");
        setTimeout(() => {
          window.location.href = "dashboard.html";
        }, 1000);
      } catch (err) {
        console.error("Sign in error", err);
        showMessage(err?.message || "Sign in failed.");
        loginBtn.disabled = false;
        loginBtn.textContent = originalText;
      }
    });
  }

  if (signupBtn) {
    signupBtn.addEventListener("click", async () => {
      const email = emailInput?.value?.trim();
      const password = passwordInput?.value || "";
      if (!email || !password) {
        showMessage("Enter email and password.");
        return;
      }
      const originalText = signupBtn.textContent;
      signupBtn.disabled = true;
      signupBtn.textContent = "Creating account...";
      showMessage("Creating account...");
      try {
        await handleSignUp(email, password);
        showMessage("Account created and signed in.");
        setTimeout(() => {
          window.location.href = "dashboard.html";
        }, 1000);
      } catch (err) {
        console.error("Sign up error", err);
        showMessage(err?.message || "Sign up failed.");
        signupBtn.disabled = false;
        signupBtn.textContent = originalText;
      }
    });
  }
}

// Wire UI after DOM is ready
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", wireUi);
} else {
  wireUi();
}


