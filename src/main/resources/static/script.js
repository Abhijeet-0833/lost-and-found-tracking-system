const API_URL = "http://localhost:8082/api/items";

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// 1. Load Items on Page Load
document.addEventListener("DOMContentLoaded", fetchItems);

async function fetchItems() {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
        const response = await fetch(API_URL, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (response.ok) {
            const items = await response.json();
            displayItems(items);
        } else {
            console.error("Failed to fetch items", await response.text());
            if (response.status === 401 || response.status === 403) {
                localStorage.clear();
                window.location.href = "index.html";
            }
        }
    } catch (err) {
        console.error("Error fetching items:", err);
    }
}

function displayItems(items) {
    const feed = document.getElementById("itemFeed");
    if (!items || !Array.isArray(items)) {
        feed.innerHTML = "<p>No items found.</p>";
        return;
    }
    
    feed.innerHTML = items.map(item => `
        <div class="card ${escapeHtml(item.itemType ? item.itemType.toLowerCase() : '')}">
            <h3>${escapeHtml(item.title)}</h3>
            ${item.photoUrl ? `<img src="http://localhost:8082${encodeURI(item.photoUrl)}" style="width:100%; border-radius:10px; margin-top:10px; margin-bottom:10px; max-height: 250px; object-fit: cover;" alt="Item Photo">` : ''}
            <p><strong>Type:</strong> ${escapeHtml(item.itemType)}</p>
            <p>${escapeHtml(item.description)}</p>
            <p>📍 ${escapeHtml(item.location)}</p>
            <div style="display:flex; justify-content:space-between; align-items:center; margin-top:15px; padding-top:10px; border-top:1px solid #f3f4f6;">
                <span class="status status-${escapeHtml(item.status || "PENDING")}">${escapeHtml(item.status || "PENDING")}</span>
                <button onclick="runMatching(${item.itemId})" class="btn-run-match">Run Matching</button>
            </div>
        </div>
    `).join('');
}

async function runMatching(itemId) {
    const token = localStorage.getItem("token");
    try {
        const response = await fetch(`http://localhost:8082/api/items/${itemId}/run-match`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        if (response.ok) {
            const message = await response.text();
            alert(message);
        } else {
            alert("Failed to run matching: " + await response.text());
        }
    } catch (err) {
        console.error(err);
        alert("Error running matching.");
    }
}

// 2. Handle Form Submission
document.getElementById("itemForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("token");
    if (!token) {
        alert("You must be logged in to report an item!");
        return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerText;
    submitBtn.disabled = true;
    submitBtn.innerText = "Submitting...";

    const formData = new FormData();
    formData.append("title", document.getElementById("title").value);
    formData.append("description", document.getElementById("description").value);
    formData.append("category", document.getElementById("category").value);
    formData.append("itemType", document.getElementById("itemType").value);
    formData.append("location", document.getElementById("location").value);
    
    const photoInput = document.getElementById("photo");
    if (photoInput.files[0]) {
        formData.append("photo", photoInput.files[0]);
    }

    try {
        const response = await fetch(`${API_URL}/report`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`
            },
            body: formData
        });

        if (response.ok) {
            alert("Reported Successfully!");
            fetchItems(); // Refresh the feed
            e.target.reset();
        } else {
            alert("Failed to report item. " + await response.text());
        }
    } catch (err) {
        console.error("Error reporting item:", err);
        alert("Error reporting item. Check console.");
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerText = originalText;
    }
});

// 3. Logout
document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    window.location.href = "index.html"; 
});

// Admin Panel redirect
if (localStorage.getItem("role") === "ADMIN") {
    const adminBtn = document.getElementById("adminPanelBtn");
    if (adminBtn) {
        adminBtn.classList.remove("hidden");
        adminBtn.style.display = "inline-block";
        adminBtn.addEventListener("click", () => {
            window.location.href = "admin.html";
        });
    }
}

// --- NEW NOTIFICATIONS AND CHAT LOGIC ---

// Polling interval
setInterval(fetchNotifications, 5000);

async function fetchNotifications() {
    const token = localStorage.getItem("token");
    if(!token) return;

    try {
        const res = await fetch("http://localhost:8082/api/notifications", { headers: {"Authorization": `Bearer ${token}`} });
        if(res.ok) {
            const notifs = await res.json();
            const count = notifs.length;
            const notifCountSpan = document.getElementById("notifCount");
            
            if(count > 0) {
                notifCountSpan.style.display = "inline";
                notifCountSpan.innerText = count;
                document.getElementById("notifList").innerHTML = notifs.map(n => `
                    <div style="padding:15px; border-bottom:1px solid rgba(255,255,255,0.05); display:flex; justify-content:space-between; align-items:center; transition:background 0.2s; border-radius:8px;">
                        <span style="font-weight:500; color:var(--text-dark);">${n.message}</span>
                        <button onclick="markNotifRead(${n.notificationId})" style="cursor:pointer; background:rgba(139,92,246,0.1); border:1px solid var(--primary); color:var(--primary); width:30px; height:30px; border-radius:50%; display:flex; align-items:center; justify-content:center; transition:all 0.2s;">✓</button>
                    </div>
                `).join('');
            } else {
                notifCountSpan.style.display = "none";
                document.getElementById("notifList").innerHTML = "<p style='padding:10px; text-align:center; color:var(--text-gray);'>No new notifications.</p>";
            }
        } else if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = "index.html";
        }
    } catch(e) { console.error(e); }
}

async function markNotifRead(id) {
    const token = localStorage.getItem("token");
    await fetch(`http://localhost:8082/api/notifications/${id}/read`, { method: "PUT", headers: {"Authorization": `Bearer ${token}`} });
    fetchNotifications();
}

document.getElementById("notificationBtn").addEventListener("click", () => {
    const drop = document.getElementById("notificationDropdown");
    drop.style.display = drop.style.display === "none" ? "block" : "none";
});

// --- CHAT SYSTEM ---
let currentMatchId = null;
let chatInterval = null;
let userMatches = [];

document.getElementById("inboxBtn").addEventListener("click", openInbox);
document.getElementById("closeInbox").addEventListener("click", () => {
    document.getElementById("inboxModal").style.display = "none";
    if(chatInterval) clearInterval(chatInterval);
});

async function openInbox() {
    const token = localStorage.getItem("token");
    document.getElementById("inboxModal").style.display = "flex";
    const res = await fetch("http://localhost:8082/api/chat/matches", { headers: {"Authorization": `Bearer ${token}`} });
    if(res.ok) {
        const matches = await res.json();
        const chatList = document.getElementById("chatList");
        
        // Filter only approved matches
        const approved = matches.filter(m => m.status === 'CHAT_APPROVED');
        userMatches = approved;
        if(approved.length === 0) {
            chatList.innerHTML = "<p style='color:var(--text-gray); text-align:center; margin-top:20px;'>No active chats.</p>";
            return;
        }

        chatList.innerHTML = approved.map(m => `
            <div onclick="openChat(${m.matchId})" style="padding:20px; border:1px solid rgba(255,255,255,0.05); cursor:pointer; background:rgba(0,0,0,0.3); border-radius:12px; margin-bottom:15px; box-shadow:0 4px 10px rgba(0,0,0,0.2); transition:transform 0.2s, border-color 0.2s;" onmouseover="this.style.transform='translateY(-2px)'; this.style.borderColor='var(--primary)';" onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='rgba(255,255,255,0.05)';">
                <div style="margin-bottom:8px;"><strong style="color:var(--warning);">Lost:</strong> <span style="color:var(--text-dark);">${m.lostItem.title}</span></div>
                <div><strong style="color:var(--success);">Found:</strong> <span style="color:var(--text-dark);">${m.foundItem.title}</span></div>
            </div>
        `).join('');
    }
}

function openChat(matchId) {
    currentMatchId = matchId;
    const match = userMatches.find(m => m.matchId === matchId);
    
    document.getElementById("chatInput").disabled = false;
    document.getElementById("sendBtn").disabled = false;
    document.getElementById("shareLocationBtn").style.display = (match && match.locationApproved) ? "block" : "none";
    document.getElementById("chatMessages").innerHTML = "<p style='text-align:center; color:var(--text-gray);'>Loading messages...</p>";
    
    if(chatInterval) clearInterval(chatInterval);
    loadMessages();
    chatInterval = setInterval(loadMessages, 3000); // poll every 3s
}

async function loadMessages() {
    if(!currentMatchId) return;
    const token = localStorage.getItem("token");
    const res = await fetch(`http://localhost:8082/api/chat/${currentMatchId}`, { headers: {"Authorization": `Bearer ${token}`} });
    if(res.ok) {
        const msgs = await res.json();
        const myUserId = parseInt(localStorage.getItem("userId") || "0"); // Might need to save userId on login, or just differentiate by senderId
        
        document.getElementById("chatMessages").innerHTML = msgs.length === 0 ? "<p style='text-align:center; color:var(--text-gray); margin-top:20px;'>No messages yet. Say hi!</p>" : 
            msgs.map(m => {
                const isMe = true; // We can improve this if we save userId in localStorage
                return `<div style="padding:12px 18px; margin-bottom:8px; background:rgba(30,41,59,0.8); border:1px solid rgba(255,255,255,0.05); border-radius:12px; box-shadow:0 2px 5px rgba(0,0,0,0.2); width:fit-content; max-width:80%;">
                    <div style="font-size:0.75rem; color:var(--primary); margin-bottom:4px; font-weight:bold; text-transform:uppercase;">User ${escapeHtml(m.senderId)}</div>
                    <div style="color:var(--text-dark); line-height:1.4;">${escapeHtml(m.content)}</div>
                </div>`;
            }).join('');
    }
}

document.getElementById("sendBtn").addEventListener("click", sendMessage);
document.getElementById("chatInput").addEventListener("keypress", (e) => { if(e.key === "Enter") sendMessage(); });
document.getElementById("shareLocationBtn").addEventListener("click", shareLocation);

async function shareLocation() {
    if(!currentMatchId) return;
    
    if(!navigator.geolocation) {
        alert("Geolocation is not supported by your browser.");
        return;
    }

    navigator.geolocation.getCurrentPosition(async (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        const content = `📍 Live Location: https://www.google.com/maps?q=${lat},${lng}`;
        
        const token = localStorage.getItem("token");
        await fetch(`http://localhost:8082/api/chat/${currentMatchId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
            body: JSON.stringify({ content })
        });
        loadMessages();
    }, () => {
        alert("Unable to retrieve your location. Please check your browser permissions.");
    });
}

async function sendMessage() {
    const input = document.getElementById("chatInput");
    const content = input.value.trim();
    if(!content || !currentMatchId) return;

    const token = localStorage.getItem("token");
    input.value = "";
    await fetch(`http://localhost:8082/api/chat/${currentMatchId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
        body: JSON.stringify({ content })
    });
    loadMessages();
}

// Initial fetch
fetchNotifications();