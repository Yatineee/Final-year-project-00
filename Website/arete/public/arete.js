// Timer for the redirect page
let pageTime = 0;
let timerInterval;
const timerElement = document.getElementById('timeCounter');

// Initialize when page loads
document.addEventListener('DOMContentLoaded', () => {
    initializeTimer();
    loadStats();
    setupEventListeners();
    incrementRedirectCount();
    updateStreak();
});

function initializeTimer() {
    timerInterval = setInterval(() => {
        pageTime++;
        timerElement.textContent = pageTime;

        // Update saved minutes (assuming 1 redirect = 5 minutes saved)
        const savedMinutes = Math.floor(pageTime / 60);
        document.getElementById('savedMinutes').textContent = savedMinutes;

        // Save current session time
        saveSessionTime(pageTime);
    }, 1000);
}

function loadStats() {
    // Load stats from localStorage
    const redirectCount = localStorage.getItem('arete_redirect_count') || 0;
    const learningStreak = localStorage.getItem('arete_learning_streak') || 1;
    const totalSavedTime = localStorage.getItem('arete_total_saved_time') || 0;

    // Update display
    document.getElementById('redirectCount').textContent = redirectCount;
    document.getElementById('learningStreak').textContent = learningStreak;

    // Calculate total saved minutes (including previous sessions)
    const totalMinutes = Math.floor((parseInt(totalSavedTime) + pageTime) / 60);
    document.getElementById('savedMinutes').textContent = totalMinutes;
}

function setupEventListeners() {
    // Study session button
    document.getElementById('startStudyBtn').addEventListener('click', startStudySession);

    // Set goal button
    document.getElementById('setGoalBtn').addEventListener('click', setDailyGoal);

    // Add click tracking to all external links
    document.querySelectorAll('a[href^="http"]').forEach(link => {
        link.addEventListener('click', trackLinkClick);
    });
}

function trackLinkClick(event) {
    const linkUrl = event.currentTarget.href;
    const linkText = event.currentTarget.textContent || event.currentTarget.querySelector('strong').textContent;

    // Save clicked link to localStorage
    const clicks = JSON.parse(localStorage.getItem('arete_link_clicks') || '[]');
    clicks.push({
        url: linkUrl,
        text: linkText,
        timestamp: new Date().toISOString()
    });
    localStorage.setItem('arete_link_clicks', JSON.stringify(clicks.slice(-50))); // Keep last 50 clicks
}

function startStudySession() {
    // Create a study session modal
    const modal = createStudyModal();
    document.body.appendChild(modal);

    // Start Pomodoro timer
    startPomodoroTimer();
}

function createStudyModal() {
    const modal = document.createElement('div');
    modal.className = 'study-modal';
    modal.innerHTML = `
        <div class="modal-overlay"></div>
        <div class="modal-content">
            <div class="modal-header">
                <h2><i class="fas fa-hourglass-start"></i> Study Session Started</h2>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                <div class="pomodoro-timer">
                    <div class="timer-display">
                        <span id="pomodoroMinutes">25</span>:<span id="pomodoroSeconds">00</span>
                    </div>
                    <div class="timer-label">FOCUS TIME</div>
                </div>
                <div class="study-tips">
                    <h3><i class="fas fa-lightbulb"></i> Tips for Effective Studying</h3>
                    <ul>
                        <li>Put your phone on silent</li>
                        <li>Close all distracting tabs</li>
                        <li>Have water nearby</li>
                        <li>Take 5-minute breaks every 25 minutes</li>
                    </ul>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-primary" id="pauseTimer">
                        <i class="fas fa-pause"></i> Pause
                    </button>
                    <button class="btn btn-secondary" id="endSession">
                        <i class="fas fa-stop"></i> End Session
                    </button>
                </div>
            </div>
        </div>
    `;

    // Add modal styles
    const style = document.createElement('style');
    style.textContent = `
        .study-modal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 1000;
        }
        
        .modal-overlay {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(5px);
        }
        
        .modal-content {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 20px;
            padding: 30px;
            width: 90%;
            max-width: 500px;
            color: white;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }
        
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
        }
        
        .modal-header h2 {
            font-size: 1.5rem;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .modal-close {
            background: none;
            border: none;
            color: white;
            font-size: 1.8rem;
            cursor: pointer;
            opacity: 0.7;
            transition: opacity 0.3s;
        }
        
        .modal-close:hover {
            opacity: 1;
        }
        
        .pomodoro-timer {
            text-align: center;
            margin: 30px 0;
        }
        
        .timer-display {
            font-size: 4rem;
            font-weight: bold;
            font-family: 'Courier New', monospace;
            background: rgba(255, 255, 255, 0.1);
            padding: 20px;
            border-radius: 15px;
            margin-bottom: 10px;
        }
        
        .timer-label {
            font-size: 1rem;
            text-transform: uppercase;
            letter-spacing: 2px;
            opacity: 0.8;
        }
        
        .study-tips {
            background: rgba(255, 255, 255, 0.05);
            padding: 20px;
            border-radius: 15px;
            margin: 25px 0;
        }
        
        .study-tips h3 {
            font-size: 1.1rem;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .study-tips ul {
            list-style: none;
            padding-left: 10px;
        }
        
        .study-tips li {
            margin-bottom: 10px;
            padding-left: 25px;
            position: relative;
        }
        
        .study-tips li:before {
            content: '✓';
            position: absolute;
            left: 0;
            color: #4facfe;
            font-weight: bold;
        }
        
        .modal-actions {
            display: flex;
            gap: 15px;
            justify-content: center;
        }
        
        .modal-actions .btn {
            padding: 12px 25px;
        }
    `;
    document.head.appendChild(style);

    // Close modal event
    modal.querySelector('.modal-close').addEventListener('click', () => {
        document.body.removeChild(modal);
        document.head.removeChild(style);
        clearInterval(window.pomodoroInterval);
    });

    // Pause timer event
    modal.querySelector('#pauseTimer').addEventListener('click', () => {
        const btn = modal.querySelector('#pauseTimer');
        const icon = btn.querySelector('i');
        const isPaused = btn.classList.contains('paused');

        if (isPaused) {
            btn.classList.remove('paused');
            icon.className = 'fas fa-pause';
            btn.innerHTML = '<i class="fas fa-pause"></i> Pause';
            startPomodoroTimer();
        } else {
            btn.classList.add('paused');
            icon.className = 'fas fa-play';
            btn.innerHTML = '<i class="fas fa-play"></i> Resume';
            clearInterval(window.pomodoroInterval);
        }
    });

    // End session event
    modal.querySelector('#endSession').addEventListener('click', () => {
        clearInterval(window.pomodoroInterval);
        const minutesStudied = 25 - parseInt(document.getElementById('pomodoroMinutes').textContent);
        alert(`Great job! You studied for ${minutesStudied} minutes. 🎉`);
        document.body.removeChild(modal);
        document.head.removeChild(style);
    });

    return modal;
}

let pomodoroTime = 25 * 60; // 25 minutes in seconds
let isPomodoroRunning = true;

function startPomodoroTimer() {
    clearInterval(window.pomodoroInterval);

    window.pomodoroInterval = setInterval(() => {
        if (isPomodoroRunning && pomodoroTime > 0) {
            pomodoroTime--;
            const minutes = Math.floor(pomodoroTime / 60);
            const seconds = pomodoroTime % 60;

            document.getElementById('pomodoroMinutes').textContent =
                minutes.toString().padStart(2, '0');
            document.getElementById('pomodoroSeconds').textContent =
                seconds.toString().padStart(2, '0');

            // Play sound when 5 minutes left
            if (pomodoroTime === 5 * 60) {
                playNotificationSound();
            }

            // Play sound when time's up
            if (pomodoroTime === 0) {
                playNotificationSound();
                alert('Time\'s up! Take a 5-minute break. ☕');
                clearInterval(window.pomodoroInterval);
            }
        }
    }, 1000);
}

function playNotificationSound() {
    // Create a simple notification sound using Web Audio API
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);

        oscillator.frequency.value = 800;
        oscillator.type = 'sine';

        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);

        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.5);
    } catch (e) {
        console.log('Audio not supported');
    }
}

function setDailyGoal() {
    const goal = prompt('What would you like to learn today?\n(e.g., "Complete 1 math lesson", "Read 20 pages")');

    if (goal) {
        localStorage.setItem('arete_daily_goal', goal);
        localStorage.setItem('arete_goal_set_date', new Date().toDateString());

        // Show confirmation
        const goalDisplay = document.createElement('div');
        goalDisplay.className = 'goal-confirmation';
        goalDisplay.innerHTML = `
            <div class="goal-content">
                <i class="fas fa-check-circle"></i>
                <div>
                    <strong>Goal set!</strong>
                    <div>"${goal}"</div>
                </div>
            </div>
        `;

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .goal-confirmation {
                position: fixed;
                bottom: 20px;
                right: 20px;
                background: linear-gradient(45deg, #4facfe 0%, #00f2fe 100%);
                color: white;
                padding: 15px 20px;
                border-radius: 10px;
                box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                z-index: 1000;
                animation: slideIn 0.3s ease-out;
            }
            
            .goal-content {
                display: flex;
                align-items: center;
                gap: 10px;
            }
            
            .goal-content i {
                font-size: 1.5rem;
            }
            
            @keyframes slideIn {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(goalDisplay);

        // Remove after 5 seconds
        setTimeout(() => {
            document.body.removeChild(goalDisplay);
            document.head.removeChild(style);
        }, 5000);
    }
}

function incrementRedirectCount() {
    const currentCount = parseInt(localStorage.getItem('arete_redirect_count') || 0);
    localStorage.setItem('arete_redirect_count', currentCount + 1);
    document.getElementById('redirectCount').textContent = currentCount + 1;
}

function updateStreak() {
    const lastVisit = localStorage.getItem('arete_last_visit');
    const today = new Date().toDateString();

    if (lastVisit !== today) {
        const streak = parseInt(localStorage.getItem('arete_learning_streak') || 0);

        // Check if yesterday
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        if (lastVisit === yesterday.toDateString()) {
            localStorage.setItem('arete_learning_streak', streak + 1);
        } else {
            localStorage.setItem('arete_learning_streak', 1);
        }

        localStorage.setItem('arete_last_visit', today);
        document.getElementById('learningStreak').textContent =
            parseInt(localStorage.getItem('arete_learning_streak') || 1);
    }
}

function saveSessionTime(currentTime) {
    const totalSavedTime = parseInt(localStorage.getItem('arete_total_saved_time') || 0);
    localStorage.setItem('arete_total_saved_time', totalSavedTime + 1);
}

// Clean up timer when page is closed
window.addEventListener('beforeunload', () => {
    if (timerInterval) {
        clearInterval(timerInterval);
    }
    if (window.pomodoroInterval) {
        clearInterval(window.pomodoroInterval);
    }

    // Save final time
    saveSessionTime(pageTime);
});