let currentRole = 'USER';

function switchRole(role) {
    currentRole = role;
    document.getElementById('userTab').classList.toggle('active', role === 'USER');
    document.getElementById('adminTab').classList.toggle('active', role === 'ADMIN');
    
    document.getElementById('userForm').classList.toggle('hidden', role !== 'USER');
    document.getElementById('adminForm').classList.toggle('hidden', role !== 'ADMIN');

    if (role === 'ADMIN') {
        document.body.classList.add('admin-mode');
    } else {
        document.body.classList.remove('admin-mode');
    }
}

// --- USER OTP LOGIC ---
async function sendOTP() {
    const email = document.getElementById('userEmail').value;
    if(!email) return alert("Please enter email");

    const res = await fetch(`http://localhost:8082/api/auth/send-otp?email=${email}`, { method: 'POST' });
    if(res.ok) {
        document.getElementById('otpSection').classList.remove('hidden');
        document.getElementById('otpBtn').classList.add('hidden');
        alert("OTP sent to your email!");
    } else {
        const data = await res.json();
        alert(data.message || "Failed to send OTP. Please try again.");
    }
}

async function verifyOTP() {
    const email = document.getElementById('userEmail').value;
    const otp = document.getElementById('otpInput').value;

    const res = await fetch(`http://localhost:8082/api/auth/verify-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, otp })
    });

    handleLoginResponse(res);
}

// --- ADMIN PASSWORD LOGIC ---
document.getElementById('adminLoginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const res = await fetch(`http://localhost:8082/api/auth/admin-login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: document.getElementById('adminEmail').value,
            password: document.getElementById('adminPassword').value
        })
    });
    handleLoginResponse(res);
});

async function handleLoginResponse(res) {
    if (res.ok) {
        const data = await res.json();
        localStorage.setItem("token", data.token);
        localStorage.setItem("role", currentRole);
        
        window.location.href = "dashboard.html";
    } else {
        const err = document.getElementById('errorMsg');
        err.innerText = "Login Failed. Please check your credentials.";
        err.style.display = "block";
    }
}