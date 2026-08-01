const API_URL = "http://localhost:8082/api/admin";
const token = localStorage.getItem("token");

if (!token || localStorage.getItem("role") !== "ADMIN") {
    window.location.href = "index.html";
}

// Navigation
document.getElementById('nav-matches').addEventListener('click', () => {
    switchTab('matches');
    loadMatches();
});
document.getElementById('nav-users').addEventListener('click', () => {
    switchTab('users');
    loadUsers();
});
document.getElementById('logoutBtn').addEventListener('click', () => {
    localStorage.clear();
    window.location.href = "index.html";
});
document.getElementById('backToDashboardBtn').addEventListener('click', () => {
    window.location.href = "dashboard.html";
});

function switchTab(tab) {
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(`nav-${tab}`).classList.add('active');
    
    document.getElementById('matchesSection').classList.add('hidden');
    document.getElementById('usersSection').classList.add('hidden');
    
    document.getElementById(`${tab}Section`).classList.remove('hidden');
    document.getElementById('pageTitle').innerText = tab === 'matches' ? 'Match Moderation' : 'User Management';
}

let userMap = {};

async function fetchUserMap() {
    try {
        const res = await fetch(`${API_URL}/users`, { headers: { "Authorization": `Bearer ${token}` } });
        if (res.ok) {
            const users = await res.json();
            users.forEach(u => userMap[u.userId] = u);
        }
    } catch(e) { console.error(e); }
}

// Load Matches
async function loadMatches() {
    await fetchUserMap();
    try {
        const res = await fetch(`${API_URL}/matches`, { headers: { "Authorization": `Bearer ${token}` } });
        if (res.ok) {
            const matches = await res.json();
            renderMatches(matches);
        }
    } catch (e) {
        console.error("Failed to load matches", e);
    }
}

function renderMatches(matches) {
    const container = document.getElementById('matchesSection');
    if (matches.length === 0) {
        container.innerHTML = "<p>No matches found.</p>";
        return;
    }

    container.innerHTML = matches.map(m => {
        const lostEmail = userMap[m.lostItem.reportedBy] ? userMap[m.lostItem.reportedBy].email : 'Unknown';
        const foundEmail = userMap[m.foundItem.reportedBy] ? userMap[m.foundItem.reportedBy].email : 'Unknown';

        return `
        <div class="match-card">
            <div class="match-info">
                <div class="item-box lost">
                    <h4>🔴 Lost Item</h4>
                    <p><strong>Title:</strong> ${m.lostItem.title}</p>
                    <p><strong>Desc:</strong> ${m.lostItem.description}</p>
                    <p><strong>Reporter Email:</strong> ${lostEmail}</p>
                </div>
                <div class="item-box found">
                    <h4>🟢 Found Item</h4>
                    <p><strong>Title:</strong> ${m.foundItem.title}</p>
                    <p><strong>Desc:</strong> ${m.foundItem.description}</p>
                    <p><strong>Reporter Email:</strong> ${foundEmail}</p>
                </div>
            </div>
            
            <div class="actions">
                <span class="status-badge status-${m.status}">${m.status}</span>
                ${m.status === 'PENDING_APPROVAL' ? `
                    <button class="btn btn-approve" onclick="updateMatchStatus(${m.matchId}, 'approve')">Enable Chat</button>
                    <button class="btn btn-block" onclick="updateMatchStatus(${m.matchId}, 'block')">Block Match</button>
                ` : ''}
                ${m.status === 'CHAT_APPROVED' ? `
                    ${!m.locationApproved ? `<button class="btn btn-approve" style="margin-bottom: 5px; width: 100%;" onclick="updateMatchStatus(${m.matchId}, 'approve-location')">Approve Live Location Sharing</button>` : `<button class="btn btn-block" style="margin-bottom: 5px; width: 100%; background: #f59e0b;" onclick="updateMatchStatus(${m.matchId}, 'block-location')">Block Location Sharing</button>`}
                    <button class="btn" style="background:transparent; border:1px solid #3b82f6; color:#3b82f6; margin-bottom: 5px; width: 100%;" onclick="viewAdminChat(${m.matchId})">View Chat Logs</button>
                    <button class="btn btn-block" style="width: 100%;" onclick="updateMatchStatus(${m.matchId}, 'block')">Revoke Chat</button>
                ` : ''}
                ${m.status === 'BLOCKED' ? `
                    <button class="btn btn-approve" onclick="updateMatchStatus(${m.matchId}, 'unblock')">Unblock Match</button>
                ` : ''}
            </div>
        </div>
        `;
    }).join('');
}

async function updateMatchStatus(id, action) {
    try {
        const res = await fetch(`${API_URL}/matches/${id}/${action}`, {
            method: 'PUT',
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.ok) {
            loadMatches();
        } else {
            alert("Failed to update status");
        }
    } catch(e) {
        console.error(e);
    }
}

// Chat Logs Viewer
document.getElementById('closeChatModal').addEventListener('click', () => {
    document.getElementById('adminChatModal').style.display = 'none';
});

async function viewAdminChat(matchId) {
    document.getElementById('adminChatModal').style.display = 'flex';
    const msgContainer = document.getElementById('adminChatMessages');
    msgContainer.innerHTML = "<p style='text-align:center;'>Loading logs...</p>";

    try {
        const res = await fetch(`${API_URL}/matches/${matchId}/chats`, { headers: { "Authorization": `Bearer ${token}` } });
        if (res.ok) {
            const msgs = await res.json();
            if (msgs.length === 0) {
                msgContainer.innerHTML = "<p style='text-align:center; color:var(--text-muted);'>No messages sent yet.</p>";
            } else {
                msgContainer.innerHTML = msgs.map(m => {
                    const senderEmail = userMap[m.senderId] ? userMap[m.senderId].email : `User ${m.senderId}`;
                    const time = new Date(m.timestamp).toLocaleString();
                    return `
                    <div style="background:rgba(255,255,255,0.05); padding:10px; border-radius:8px; border:1px solid #334155;">
                        <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                            <strong style="color:var(--accent);">${senderEmail}</strong>
                            <span style="font-size:0.8rem; color:var(--text-muted);">${time}</span>
                        </div>
                        <div>${m.content}</div>
                    </div>`;
                }).join('');
            }
        }
    } catch (e) {
        console.error(e);
    }
}

// Load Users
async function loadUsers() {
    try {
        const res = await fetch(`${API_URL}/users`, { headers: { "Authorization": `Bearer ${token}` } });
        if (res.ok) {
            const users = await res.json();
            const tbody = document.getElementById('userTableBody');
            tbody.innerHTML = users.map(u => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                <td>${u.userId}</td>
                <td>${u.email}</td>
                <td>${u.username}</td>
                <td>
                    <span class="status-badge ${u.role === 'ADMIN' ? 'status-CHAT_APPROVED' : (u.role === 'BLOCKED' ? 'status-BLOCKED' : 'status-PENDING_APPROVAL')}">
                        ${u.role}
                    </span>
                </td>
                <td>
                    ${u.role !== 'ADMIN' ? `
                        ${u.role === 'BLOCKED' ? 
                            `<button class="btn btn-approve" onclick="updateUserStatus(${u.userId}, 'unblock')" style="margin-right:10px;">Unblock</button>` :
                            `<button class="btn" style="background:#f59e0b; color:white; margin-right:10px;" onclick="updateUserStatus(${u.userId}, 'block')">Block</button>`
                        }
                        <button class="btn btn-block" onclick="deleteUser(${u.userId})">Delete</button>
                    ` : '<span style="color:var(--text-muted);">System Admin</span>'}
                </td>
            `;
            return tr.outerHTML;
            }).join('');
        }
    } catch (e) {
        console.error("Error deleting user:", e);
    }
}

async function updateUserStatus(userId, action) {
    if (!confirm(`Are you sure you want to ${action} this user?`)) return;

    try {
        const res = await fetch(`${API_URL}/users/${userId}/${action}`, {
            method: 'PUT',
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        if (res.ok) {
            alert(`User ${action}ed successfully.`);
            loadUsers();
        } else {
            alert("Failed to update user status.");
        }
    } catch (e) {
        console.error("Error updating user status:", e);
    }
}

async function deleteUser(id) {
    if(!confirm("Are you sure you want to completely delete this user?")) return;
    try {
        const res = await fetch(`${API_URL}/users/${id}`, {
            method: 'DELETE',
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.ok) {
            loadUsers();
        }
    } catch(e) {
        console.error(e);
    }
}

// --- REPORTS MANAGEMENT ---
let allReports = [];

document.getElementById('nav-reports').addEventListener('click', () => {
    switchTab('reports');
    loadReports();
});

async function loadReports() {
    await fetchUserMap();
    try {
        // Items endpoint returns ALL items if admin
        const res = await fetch(`http://localhost:8082/api/items`, { headers: { "Authorization": `Bearer ${token}` } });
        if (res.ok) {
            allReports = await res.json();
            renderReports(allReports);
        }
    } catch (e) {
        console.error("Failed to load reports", e);
    }
}

function filterReports(status) {
    if (status === 'ALL') {
        renderReports(allReports);
    } else {
        renderReports(allReports.filter(r => r.status === status));
    }
}

function renderReports(reports) {
    const container = document.getElementById('reportsGrid');
    if (reports.length === 0) {
        container.innerHTML = "<p>No reports found.</p>";
        return;
    }

    container.innerHTML = reports.map(r => {
        const reporterEmail = userMap[r.reportedBy] ? userMap[r.reportedBy].email : 'Unknown';
        const typeColor = r.itemType === 'LOST' ? 'var(--warning)' : 'var(--success)';
        
        return `
        <div class="card ${r.itemType.toLowerCase()}" style="margin-bottom:20px; border-left: 4px solid ${r.itemType==='LOST'?'#f59e0b':'#10b981'};">
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                <h3 style="color:${r.itemType==='LOST'?'#f59e0b':'#10b981'}; font-size:1.3rem; margin:0 0 10px 0;">
                    ${r.itemType} ITEM: ${r.title}
                </h3>
                <span style="font-size:0.8rem; padding:4px 8px; background:rgba(255,255,255,0.1); border-radius:4px; text-transform:uppercase;">
                    ${r.status}
                </span>
            </div>
            ${r.photoUrl ? `<img src="http://localhost:8082${r.photoUrl}" style="width:100%; border-radius:10px; margin-bottom:10px; max-height: 250px; object-fit: cover;" alt="Item Photo">` : ''}
            <div style="color:var(--text-gray); font-size:0.95rem; line-height:1.6;">
                <strong>Description:</strong> ${r.description}<br>
                <strong>Category:</strong> ${r.category}<br>
                <strong>Location:</strong> ${r.location}<br>
                <strong>Reporter Email:</strong> ${reporterEmail}<br>
                <strong>Date:</strong> ${new Date(r.createdAt).toLocaleString()}
            </div>
            
            <div class="actions" style="flex-direction:row; gap:10px; margin-top:15px; border-top:1px solid #334155; padding-top:15px;">
                <button class="btn btn-approve" onclick="updateReportStatus(${r.itemId}, 'APPROVED')">Mark Approved</button>
                <button class="btn btn-approve" onclick="updateReportStatus(${r.itemId}, 'SOLVED')">Mark Solved</button>
                <button class="btn btn-block" onclick="deleteReportItem(${r.itemId})">Delete Permanently</button>
            </div>
        </div>
        `;
    }).join('');
}

async function updateReportStatus(id, newStatus) {
    if (newStatus === 'REJECTED') {
        if(!confirm("Are you sure you want to reject/hide this report?")) return;
    }
    try {
        const res = await fetch(`http://localhost:8082/api/items/${id}/status?status=${newStatus}`, {
            method: 'PUT',
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.ok) {
            loadReports();
        } else {
            alert("Failed to update report status");
        }
    } catch(e) {
        console.error(e);
    }
}

async function deleteReportItem(id) {
    if(!confirm("Are you sure you want to permanently delete this report?")) return;
    try {
        const res = await fetch(`http://localhost:8082/api/admin/items/${id}`, {
            method: 'DELETE',
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (res.ok) {
            loadReports();
        } else {
            alert("Failed to delete report.");
        }
    } catch(e) {
        console.error(e);
    }
}

// Ensure the switchTab function supports the reports section
const originalSwitchTab = switchTab;
window.switchTab = function(tab) {
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(`nav-${tab}`).classList.add('active');
    
    document.getElementById('matchesSection').classList.add('hidden');
    document.getElementById('usersSection').classList.add('hidden');
    document.getElementById('reportsSection').classList.add('hidden'); // Add this line
    
    document.getElementById(`${tab}Section`).classList.remove('hidden');
    
    let title = 'Match Moderation';
    if(tab === 'users') title = 'User Management';
    if(tab === 'reports') title = 'Reports Management';
    document.getElementById('pageTitle').innerText = title;
}

// Init
document.addEventListener("DOMContentLoaded", loadMatches);
