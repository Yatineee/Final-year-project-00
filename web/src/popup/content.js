const HOST_TO_APP = {
    "www.instagram.com": "instagram",
    "instagram.com": "instagram",
    "www.facebook.com": "facebook",
    "facebook.com": "facebook",
    "www.x.com": "twitter",
    "x.com": "twitter",
    "www.reddit.com": "reddit",
    "reddit.com": "reddit",
    "www.tiktok.com": "tiktok",
    "tiktok.com": "tiktok",
    "www.youtube.com": "youtube",
    "youtube.com": "youtube",
    "www.snapchat.com": "snapchat",
    "snapchat.com": "snapchat",
};

const appId = HOST_TO_APP[location.hostname];

// Check if user has exceeded daily goal and redirect to dashboard
async function checkDailyGoalAndBlock() {
    try {
        const userId = await chrome.runtime.sendMessage({ type: "GET_USER_ID" });
        if (!userId) return;

        const stats = await chrome.runtime.sendMessage({ type: "GET_STATS", userId });
        const { totalTimeSpent, dailyGoalMinutes } = stats;

        // If total time spent >= daily goal, redirect to dashboard
        if (totalTimeSpent >= dailyGoalMinutes) {
            const dashboardUrl = await chrome.runtime.sendMessage({ type: "GET_EXTENSION_URL" });
            location.replace(dashboardUrl);
        }
    } catch (error) {
        console.error("Error checking daily goal:", error);
    }
}

// Check on page load
checkDailyGoalAndBlock();

if (appId && chrome?.runtime?.sendMessage) {
    let timeSpentSeconds = 0; // total visible seconds this session
    let lastSentSeconds = 0;  // last total synced to background
    let visible = document.visibilityState === "visible";

    // Count one second when tab is visible
    setInterval(() => {
        if (visible) timeSpentSeconds += 1;
    }, 1000);

    // Flush delta every 5 seconds
    const flush = () => {
        const delta = timeSpentSeconds - lastSentSeconds;
        if (delta > 0) {
            chrome.runtime.sendMessage({
                type: "USAGE_DELTA",
                appId,
                deltaSeconds: delta,
                date: new Date().toISOString().slice(0, 10),
            });
            lastSentSeconds = timeSpentSeconds;
        }
    };

    setInterval(flush, 5000);

    document.addEventListener("visibilitychange", () => {
        flush();
        visible = document.visibilityState === "visible";
    });

    window.addEventListener("beforeunload", flush);
}