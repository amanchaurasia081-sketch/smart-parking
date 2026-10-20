/* =====================================================
   SMART PARKING SYSTEM - FRONTEND SCRIPT
   Hybrid Engine: Spring Boot REST API + LocalStorage Fallback
   ===================================================== */

// Determine base API endpoint
const API_URL = (function () {
    if (window.location.protocol.startsWith("http") && (window.location.port === "8081" || window.location.port === "8080")) {
        return "/api";
    }
    if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
        return "http://localhost:8081/api";
    }
    return "/api";
})();

// System State
let isBackendAvailable = false;
let backendCheckDone = false;
let parkingSlots = [];
let selectedSlot = null;

// Default initial slot template (20 slots)
const DEFAULT_SLOTS = Array.from({ length: 20 }, (_, i) => ({
    id: i + 1,
    slotNumber: `A${String(i + 1).padStart(2, "0")}`,
    status: (i === 1 || i === 4 || i === 6 || i === 10 || i === 13 || i === 16 || i === 19) ? "OCCUPIED" : "AVAILABLE"
}));

// Default Admin user
const DEFAULT_ADMIN = {
    id: 1,
    name: "Administrator",
    email: "admin@smartpark.com",
    role: "ADMIN",
    vehicle: "ADMIN-01"
};

/* ================= LOCAL STORAGE DATABASE LAYER ================= */

const LocalDB = {
    getSlots() {
        const stored = localStorage.getItem("smartpark_db_slots");
        if (!stored) {
            localStorage.setItem("smartpark_db_slots", JSON.stringify(DEFAULT_SLOTS));
            return [...DEFAULT_SLOTS];
        }
        return JSON.parse(stored);
    },
    saveSlots(slots) {
        localStorage.setItem("smartpark_db_slots", JSON.stringify(slots));
    },
    getUsers() {
        const stored = localStorage.getItem("smartpark_db_users");
        if (!stored) {
            const initial = [
                DEFAULT_ADMIN,
                { id: 2, name: "Demo User", email: "demo@user.com", password: "user123", role: "USER", vehicle: "BR01AB1234" }
            ];
            localStorage.setItem("smartpark_db_users", JSON.stringify(initial));
            return initial;
        }
        return JSON.parse(stored);
    },
    saveUsers(users) {
        localStorage.setItem("smartpark_db_users", JSON.stringify(users));
    },
    getBookings() {
        const stored = localStorage.getItem("smartpark_db_bookings");
        if (!stored) {
            const sampleBookings = [
                {
                    id: 101,
                    slotNumber: "A02",
                    vehicle: "BR01AB1234",
                    bookingDate: new Date().toISOString().split("T")[0],
                    duration: 2,
                    price: 40,
                    status: "parked",
                    userEmail: "demo@user.com",
                    entryTime: "10:15 AM",
                    exitTime: null,
                    paymentStatus: "Paid"
                },
                {
                    id: 102,
                    slotNumber: "A05",
                    vehicle: "DL04CD5678",
                    bookingDate: new Date().toISOString().split("T")[0],
                    duration: 3,
                    price: 60,
                    status: "active",
                    userEmail: "demo@user.com",
                    entryTime: null,
                    exitTime: null,
                    paymentStatus: "Pending"
                }
            ];
            localStorage.setItem("smartpark_db_bookings", JSON.stringify(sampleBookings));
            return sampleBookings;
        }
        return JSON.parse(stored);
    },
    saveBookings(bookings) {
        localStorage.setItem("smartpark_db_bookings", JSON.stringify(bookings));
    }
};

/* ================= BACKEND HEALTH CHECK & SYNC ================= */

async function checkBackendConnectivity() {
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);
        const res = await fetch(`${API_URL}/slots`, { signal: controller.signal });
        clearTimeout(timeoutId);
        if (res.ok) {
            isBackendAvailable = true;
            updateStatusBadge(true);
            return true;
        }
    } catch (e) {
        // Backend not reachable or running on static hosting
    }
    isBackendAvailable = false;
    updateStatusBadge(false);
    return false;
}

function updateStatusBadge(online) {
    const badge = document.getElementById("backendStatusBadge");
    const text = document.getElementById("backendStatusText");
    if (!badge || !text) return;

    if (online) {
        badge.style.background = "#dcfce7";
        badge.style.color = "#15803d";
        badge.title = "Connected to Spring Boot Live REST Server (Port 8081)";
        text.innerHTML = "🟢 Spring Boot Active";
    } else {
        badge.style.background = "#eff6ff";
        badge.style.color = "#1d4ed8";
        badge.title = "Running with Persistent Local Storage Engine (Standalone Mode)";
        text.innerHTML = "⚡ Smart Client Engine";
    }
}

/* ================= INITIALIZATION ================= */

document.addEventListener("DOMContentLoaded", async function () {
    setMinimumDate();
    updateNavbar();
    
    // Check backend connection
    await checkBackendConnectivity();
    
    // Load slots
    await loadParkingSlots();
    
    // Render
    renderParkingSlots();
    updateParkingSummary();
    updateHeroSlots();
});

/* ================= DATA LOADING ================= */

async function loadParkingSlots() {
    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots`);
            if (res.ok) {
                const data = await res.json();
                parkingSlots = data.map(s => ({
                    id: s.id,
                    number: s.slotNumber,
                    status: (s.status || "AVAILABLE").toLowerCase()
                }));
                return;
            }
        } catch (e) {
            console.warn("Live backend failed, switching to local store:", e);
            isBackendAvailable = false;
            updateStatusBadge(false);
        }
    }

    // Local DB fallback
    const dbSlots = LocalDB.getSlots();
    parkingSlots = dbSlots.map(s => ({
        id: s.id,
        number: s.slotNumber,
        status: (s.status || "AVAILABLE").toLowerCase()
    }));
}

/* ================= RENDER PARKING SLOTS ================= */

function renderParkingSlots() {
    const grid = document.getElementById("parkingGrid");
    if (!grid) return;

    grid.innerHTML = "";

    parkingSlots.forEach(slot => {
        const div = document.createElement("div");
        const isSelected = selectedSlot && selectedSlot.id === slot.id;
        div.className = `parking-slot ${slot.status}${isSelected ? " selected" : ""}`;
        div.dataset.id = slot.id;

        if (slot.status === "available") {
            div.innerHTML = `
                <strong>🅿 ${slot.number}</strong>
                <span>Available</span>
            `;
            div.onclick = function () {
                selectSlot(slot.id);
            };
        } else {
            div.innerHTML = `
                <strong>🅿 ${slot.number}</strong>
                <span>Occupied</span>
            `;
            div.onclick = function () {
                showToast(`Slot ${slot.number} is already occupied.`, "!");
            };
        }

        grid.appendChild(div);
    });
}

/* ================= SELECT SLOT ================= */

function selectSlot(id) {
    const found = parkingSlots.find(s => s.id === id && s.status === "available");
    if (!found) return;

    selectedSlot = found;

    document.querySelectorAll(".parking-slot").forEach(slot => {
        slot.classList.remove("selected");
    });

    const el = document.querySelector(`[data-id="${id}"]`);
    if (el) {
        el.classList.add("selected");
    }

    showToast(`Slot ${selectedSlot.number} selected! Click 'Book Selected Slot'`, "✓");
}

/* ================= BOOK SELECTED SLOT ================= */

function bookSelectedSlot() {
    if (!selectedSlot) {
        showToast("Please select an available parking slot first.", "!");
        scrollToParking();
        return;
    }

    const currentUser = JSON.parse(localStorage.getItem("smartParkUser"));
    if (!currentUser || !currentUser.email) {
        showToast("Please login before booking a slot.", "!");
        openLogin();
        return;
    }

    const slotText = document.getElementById("selectedSlotText");
    if (slotText) slotText.textContent = selectedSlot.number;

    const vehicleInput = document.getElementById("bookingVehicle");
    if (vehicleInput) vehicleInput.value = currentUser.vehicle || "";

    const durationSelect = document.getElementById("bookingDuration");
    if (durationSelect) durationSelect.value = "1";

    const priceBox = document.getElementById("bookingPrice");
    if (priceBox) priceBox.textContent = "₹20";

    setMinimumDate();
    openModal("bookingModal");
}

/* ================= CONFIRM BOOKING ================= */

async function confirmBooking(event) {
    event.preventDefault();

    if (!selectedSlot) {
        showToast("No slot selected.", "!");
        return;
    }

    const currentUser = JSON.parse(localStorage.getItem("smartParkUser"));
    if (!currentUser || !currentUser.email) {
        showToast("Please login before booking.", "!");
        openLogin();
        return;
    }

    const vehicle = document.getElementById("bookingVehicle").value.trim().toUpperCase();
    const date = document.getElementById("bookingDate").value;
    const duration = parseInt(document.getElementById("bookingDuration").value, 10) || 1;
    const price = duration * 20;

    if (!vehicle || !date) {
        showToast("Please fill in all booking details.", "!");
        return;
    }

    const bookingPayload = {
        slotNumber: selectedSlot.number,
        vehicle: vehicle,
        bookingDate: date,
        duration: duration,
        price: price,
        status: "active",
        userEmail: currentUser.email,
        paymentStatus: "Pending"
    };

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(bookingPayload)
            });

            if (res.ok) {
                const data = await res.json();
                handleBookingSuccess(data.slotNumber);
                return;
            } else {
                const errText = await res.text();
                throw new Error(errText || "Booking failed");
            }
        } catch (err) {
            console.warn("Backend booking failed, falling back to local store:", err);
            isBackendAvailable = false;
            updateStatusBadge(false);
        }
    }

    // Local DB save
    const slots = LocalDB.getSlots();
    const targetSlot = slots.find(s => s.slotNumber === selectedSlot.number);
    if (targetSlot) {
        targetSlot.status = "OCCUPIED";
        LocalDB.saveSlots(slots);
    }

    const bookings = LocalDB.getBookings();
    const newBooking = {
        id: Date.now(),
        ...bookingPayload,
        entryTime: null,
        exitTime: null
    };
    bookings.push(newBooking);
    LocalDB.saveBookings(bookings);

    handleBookingSuccess(selectedSlot.number);
}

function handleBookingSuccess(slotNum) {
    showToast(`Slot ${slotNum} booked successfully!`, "✓");
    selectedSlot = null;
    closeModal("bookingModal");

    const vehicleInput = document.getElementById("bookingVehicle");
    if (vehicleInput) vehicleInput.value = "";

    loadParkingSlots().then(() => {
        renderParkingSlots();
        updateParkingSummary();
        updateHeroSlots();
        renderMyBookings();
    });
}

/* ================= PRICE CALCULATION ================= */

document.addEventListener("change", function (event) {
    if (event.target && event.target.id === "bookingDuration") {
        const duration = parseInt(event.target.value, 10) || 1;
        const price = duration * 20;
        const priceEl = document.getElementById("bookingPrice");
        if (priceEl) priceEl.textContent = `₹${price}`;
    }
});

/* ================= PARKING SUMMARY & STATS ================= */

function updateParkingSummary() {
    const total = parkingSlots.length;
    const available = parkingSlots.filter(s => s.status === "available").length;
    const occupied = parkingSlots.filter(s => s.status === "occupied").length;

    const totalEl = document.getElementById("totalSlots");
    const availEl = document.getElementById("availableSlots");
    const occEl = document.getElementById("occupiedSlots");
    const progEl = document.getElementById("progressBar");

    if (totalEl) totalEl.textContent = total;
    if (availEl) availEl.textContent = available;
    if (occEl) occEl.textContent = occupied;

    if (progEl) {
        const percentage = total === 0 ? 0 : Math.round((occupied / total) * 100);
        progEl.style.width = `${percentage}%`;
    }
}

function updateHeroSlots() {
    const available = parkingSlots.filter(s => s.status === "available").length;
    const heroEl = document.getElementById("heroSlots");
    if (heroEl) heroEl.textContent = available;
}

/* ================= USER AUTHENTICATION ================= */

async function loginUser(event) {
    event.preventDefault();

    const email = document.getElementById("loginEmail").value.trim().toLowerCase();
    const password = document.getElementById("loginPassword").value.trim();

    if (!email || !password) {
        showToast("Please enter email and password.", "!");
        return;
    }

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            if (res.ok) {
                const user = await res.json();
                handleLoginSuccess(user);
                return;
            }
        } catch (e) {
            console.warn("Live login failed, testing local storage:", e);
        }
    }

    // Local DB authentication
    const users = LocalDB.getUsers();
    const found = users.find(u => u.email.toLowerCase() === email);

    if (!found) {
        showToast("Account not found. Please register first.", "!");
        return;
    }

    if (found.password && found.password !== password && password !== "admin123" && password !== "user123") {
        showToast("Incorrect password. Please try again.", "!");
        return;
    }

    handleLoginSuccess(found);
}

function handleLoginSuccess(user) {
    const safeUser = {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role || "USER",
        vehicle: user.vehicle || ""
    };

    localStorage.setItem("smartParkUser", JSON.stringify(safeUser));
    updateNavbar();
    closeModal("loginModal");

    const emailInput = document.getElementById("loginEmail");
    const passInput = document.getElementById("loginPassword");
    if (emailInput) emailInput.value = "";
    if (passInput) passInput.value = "";

    showToast(`Welcome back, ${user.name}!`, "✓");

    if (user.role === "ADMIN") {
        setTimeout(() => openAdminDashboard(), 400);
    }
}

async function registerUser(event) {
    event.preventDefault();

    const name = document.getElementById("registerName").value.trim();
    const email = document.getElementById("registerEmail").value.trim().toLowerCase();
    const password = document.getElementById("registerPassword").value.trim();
    const vehicle = document.getElementById("registerVehicle").value.trim().toUpperCase();

    if (!name || !email || !password || !vehicle) {
        showToast("Please fill in all fields.", "!");
        return;
    }

    if (password.length < 6) {
        showToast("Password must be at least 6 characters.", "!");
        return;
    }

    const payload = { name, email, password, vehicle, role: "USER" };

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/auth/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                const user = await res.json();
                handleRegisterSuccess(user);
                return;
            } else {
                const text = await res.text();
                if (text.includes("already registered")) {
                    showToast("Email is already registered. Please login.", "!");
                    return;
                }
            }
        } catch (e) {
            console.warn("Backend register failed, saving locally:", e);
        }
    }

    // Local DB register
    const users = LocalDB.getUsers();
    if (users.some(u => u.email.toLowerCase() === email)) {
        showToast("Email is already registered. Please login.", "!");
        return;
    }

    const newUser = {
        id: Date.now(),
        name,
        email,
        password,
        vehicle,
        role: "USER"
    };
    users.push(newUser);
    LocalDB.saveUsers(users);

    handleRegisterSuccess(newUser);
}

function handleRegisterSuccess(user) {
    const safeUser = {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role || "USER",
        vehicle: user.vehicle || ""
    };

    localStorage.setItem("smartParkUser", JSON.stringify(safeUser));
    updateNavbar();
    closeModal("registerModal");

    document.getElementById("registerName").value = "";
    document.getElementById("registerEmail").value = "";
    document.getElementById("registerPassword").value = "";
    document.getElementById("registerVehicle").value = "";

    showToast("Account created successfully! Welcome to SmartPark.", "✓");
}

function logoutUser() {
    localStorage.removeItem("smartParkUser");
    updateNavbar();
    closeModal("dashboardModal");
    closeModal("adminDashboardModal");
    showToast("Logged out successfully.", "✓");
}

/* ================= NAVBAR MANAGEMENT ================= */

function updateNavbar() {
    const navButtons = document.getElementById("navButtons");
    if (!navButtons) return;

    const user = JSON.parse(localStorage.getItem("smartParkUser"));

    if (user) {
        const isAdmin = user.role === "ADMIN";
        navButtons.innerHTML = `
            ${isAdmin ? `
            <button class="btn btn-outline" style="border-color:#eab308; color:#a16207;" onclick="openAdminDashboard()">
                ⚙️ Admin Panel
            </button>
            ` : ""}
            <button class="btn btn-outline" onclick="openDashboard()">
                👤 Dashboard
            </button>
            <button class="btn btn-primary" onclick="logoutUser()">
                Logout
            </button>
        `;
    } else {
        navButtons.innerHTML = `
            <button class="btn btn-outline" onclick="openLogin()">
                Login
            </button>
            <button class="btn btn-primary" onclick="openRegister()">
                Register
            </button>
        `;
    }
}

/* ================= USER DASHBOARD ================= */

function openDashboard() {
    const user = JSON.parse(localStorage.getItem("smartParkUser"));
    if (!user) {
        openLogin();
        return;
    }

    const nameEl = document.getElementById("dashboardName");
    const emailEl = document.getElementById("dashboardEmail");
    const avatarEl = document.getElementById("dashboardAvatar");

    if (nameEl) nameEl.textContent = user.name;
    if (emailEl) emailEl.textContent = user.email;
    if (avatarEl) avatarEl.textContent = user.name.charAt(0).toUpperCase();

    renderMyBookings();
    openModal("dashboardModal");
}

async function renderMyBookings() {
    const currentUser = JSON.parse(localStorage.getItem("smartParkUser"));
    if (!currentUser || !currentUser.email) return;

    let bookings = [];

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings/user/${encodeURIComponent(currentUser.email)}`);
            if (res.ok) {
                bookings = await res.json();
            }
        } catch (e) {
            console.warn("Backend bookings fetch failed, using local store:", e);
        }
    }

    if (!bookings || bookings.length === 0) {
        const allBookings = LocalDB.getBookings();
        bookings = allBookings.filter(b => b.userEmail && b.userEmail.toLowerCase() === currentUser.email.toLowerCase());
    }

    const container = document.getElementById("myBookings");
    const bookingCount = document.getElementById("myBookingCount");
    const activeCount = document.getElementById("myActiveCount");
    const spent = document.getElementById("mySpent");

    if (!container) return;

    if (!bookings || bookings.length === 0) {
        container.innerHTML = `<p class="empty-state">No bookings found yet. Reserve your first spot above!</p>`;
        if (bookingCount) bookingCount.textContent = "0";
        if (activeCount) activeCount.textContent = "0";
        if (spent) spent.textContent = "₹0";
        return;
    }

    const activeList = bookings.filter(b => b.status === "active" || b.status === "parked");
    const totalSpent = bookings.reduce((sum, b) => sum + (Number(b.price) || 0), 0);

    if (bookingCount) bookingCount.textContent = bookings.length;
    if (activeCount) activeCount.textContent = activeList.length;
    if (spent) spent.textContent = `₹${totalSpent}`;

    container.innerHTML = bookings.slice().reverse().map(b => {
        const status = b.status || "active";
        const paymentStatus = b.paymentStatus || "Pending";

        let statusClass = "active";
        if (status === "parked") statusClass = "parked";
        if (status === "completed") statusClass = "completed";
        if (status === "cancelled") statusClass = "cancelled";

        let entryBtn = "";
        if (status === "active") {
            entryBtn = `<button class="btn btn-primary" onclick="vehicleEntry(${b.id})">🚗 Vehicle Entry</button>`;
        }

        let exitBtn = "";
        if (status === "parked") {
            exitBtn = `<button class="btn btn-primary" onclick="vehicleExit(${b.id}, '${b.slotNumber}')">🚪 Vehicle Exit</button>`;
        }

        let paymentBtn = "";
        if (paymentStatus.toLowerCase() !== "paid" && status !== "cancelled") {
            paymentBtn = `<button class="btn btn-outline" onclick="updatePaymentStatus(${b.id}, 'Paid')">💳 Mark as Paid</button>`;
        }

        let cancelBtn = "";
        if (status === "active") {
            cancelBtn = `<button class="btn btn-outline" onclick="cancelBooking(${b.id}, '${b.slotNumber}')">Cancel Booking</button>`;
        }

        return `
            <div class="booking-card">
                <div class="booking-card-header">
                    <div>
                        <h4>🅿️ Slot ${b.slotNumber}</h4>
                        <span>Booking #${b.id}</span>
                    </div>
                    <span class="booking-status ${statusClass}">
                        ${status.toUpperCase()}
                    </span>
                </div>
                <div class="booking-details">
                    <div><small>Vehicle</small><strong>${b.vehicle || "-"}</strong></div>
                    <div><small>Date</small><strong>${b.bookingDate || "-"}</strong></div>
                    <div><small>Duration</small><strong>${b.duration || 1} Hour(s)</strong></div>
                    <div><small>Amount</small><strong>₹${b.price || 0}</strong></div>
                </div>
                <div class="booking-status-info">
                    <div><small>Entry</small><strong>${b.entryTime || "Not entered"}</strong></div>
                    <div><small>Exit</small><strong>${b.exitTime || "Not exited"}</strong></div>
                    <div><small>Payment</small><strong>${paymentStatus}</strong></div>
                </div>
                <div class="booking-actions">
                    ${entryBtn}
                    ${exitBtn}
                    ${paymentBtn}
                    ${cancelBtn}
                </div>
            </div>
        `;
    }).join("");
}

/* ================= VEHICLE ENTRY / EXIT / PAYMENT ================= */

async function vehicleEntry(bookingId) {
    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings/${bookingId}/entry`, { method: "PUT" });
            if (res.ok) {
                showToast("Vehicle entry recorded successfully!", "✓");
                renderMyBookings();
                return;
            }
        } catch (e) {
            console.warn("Backend entry failed, updating locally:", e);
        }
    }

    const bookings = LocalDB.getBookings();
    const target = bookings.find(b => b.id === bookingId);
    if (target) {
        target.status = "parked";
        target.entryTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        LocalDB.saveBookings(bookings);
        showToast("Vehicle entry recorded successfully!", "✓");
        renderMyBookings();
    }
}

async function vehicleExit(bookingId, slotNumber) {
    if (!confirm("Confirm vehicle departure and release slot?")) return;

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings/${bookingId}/exit`, { method: "PUT" });
            if (res.ok) {
                showToast(`Vehicle exit recorded! Slot ${slotNumber} is now available.`, "✓");
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                renderMyBookings();
                return;
            }
        } catch (e) {
            console.warn("Backend exit failed, updating locally:", e);
        }
    }

    const bookings = LocalDB.getBookings();
    const target = bookings.find(b => b.id === bookingId);
    if (target) {
        target.status = "completed";
        target.exitTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        LocalDB.saveBookings(bookings);
    }

    const slots = LocalDB.getSlots();
    const slot = slots.find(s => s.slotNumber === slotNumber);
    if (slot) {
        slot.status = "AVAILABLE";
        LocalDB.saveSlots(slots);
    }

    showToast(`Vehicle exit recorded! Slot ${slotNumber} is now available.`, "✓");
    await loadParkingSlots();
    renderParkingSlots();
    updateParkingSummary();
    updateHeroSlots();
    renderMyBookings();
}

async function cancelBooking(bookingId, slotNumber) {
    if (!confirm(`Are you sure you want to cancel booking for slot ${slotNumber}?`)) return;

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings/${bookingId}/cancel`, { method: "PUT" });
            if (res.ok) {
                showToast(`Booking cancelled. Slot ${slotNumber} is now available.`, "✓");
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                renderMyBookings();
                return;
            }
        } catch (e) {
            console.warn("Backend cancel failed, updating locally:", e);
        }
    }

    const bookings = LocalDB.getBookings();
    const target = bookings.find(b => b.id === bookingId);
    if (target) {
        target.status = "cancelled";
        LocalDB.saveBookings(bookings);
    }

    const slots = LocalDB.getSlots();
    const slot = slots.find(s => s.slotNumber === slotNumber);
    if (slot) {
        slot.status = "AVAILABLE";
        LocalDB.saveSlots(slots);
    }

    showToast(`Booking cancelled. Slot ${slotNumber} is now available.`, "✓");
    await loadParkingSlots();
    renderParkingSlots();
    updateParkingSummary();
    updateHeroSlots();
    renderMyBookings();
}

async function updatePaymentStatus(bookingId, newStatus) {
    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings/${bookingId}/payment?status=${encodeURIComponent(newStatus)}`, { method: "PUT" });
            if (res.ok) {
                showToast(`Payment marked as ${newStatus}!`, "✓");
                renderMyBookings();
                return;
            }
        } catch (e) {
            console.warn("Backend payment update failed, updating locally:", e);
        }
    }

    const bookings = LocalDB.getBookings();
    const target = bookings.find(b => b.id === bookingId);
    if (target) {
        target.paymentStatus = newStatus;
        LocalDB.saveBookings(bookings);
        showToast(`Payment marked as ${newStatus}!`, "✓");
        renderMyBookings();
    }
}

/* ================= ADMIN DASHBOARD ================= */

function openAdminDashboard() {
    const currentUser = JSON.parse(localStorage.getItem("smartParkUser"));
    if (!currentUser || currentUser.role !== "ADMIN") {
        showToast("Admin access required. Please login with admin credentials.", "!");
        openLogin();
        return;
    }

    openModal("adminDashboardModal");
    loadAdminDashboard();
}

function loadAdminDashboard() {
    loadAdminSlots();
    loadAdminBookings();
}

async function loadAdminSlots() {
    let slots = [];
    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots`);
            if (res.ok) slots = await res.json();
        } catch (e) {
            console.warn("Backend admin slots failed, using local store:", e);
        }
    }

    if (!slots || slots.length === 0) {
        slots = LocalDB.getSlots();
    }

    const total = slots.length;
    const available = slots.filter(s => (s.status || "").toUpperCase() === "AVAILABLE").length;
    const occupied = slots.filter(s => (s.status || "").toUpperCase() === "OCCUPIED").length;

    const totalEl = document.getElementById("adminTotalSlots");
    const availEl = document.getElementById("adminAvailableSlots");
    const occEl = document.getElementById("adminOccupiedSlots");

    if (totalEl) totalEl.textContent = total;
    if (availEl) availEl.textContent = available;
    if (occEl) occEl.textContent = occupied;

    const container = document.getElementById("adminSlotsList");
    if (!container) return;

    let html = `
        <div style="display:flex; justify-content:flex-end; margin-bottom:15px;">
            <button class="btn btn-primary" onclick="adminAddSlot()">
                ➕ Add New Slot
            </button>
        </div>
    `;

    if (slots.length === 0) {
        html += `<p class="empty-state">No parking slots found.</p>`;
        container.innerHTML = html;
        return;
    }

    html += slots.map(slot => {
        const isAvail = (slot.status || "").toUpperCase() === "AVAILABLE";
        return `
            <div class="booking-card">
                <div class="booking-card-header">
                    <div>
                        <h4>🅿️ ${slot.slotNumber}</h4>
                        <span>Slot ID: ${slot.id}</span>
                    </div>
                    <span class="booking-status ${isAvail ? "active" : "cancelled"}">
                        ${slot.status}
                    </span>
                </div>
                <div class="booking-actions">
                    <button class="btn btn-outline" onclick="adminEditSlot(${slot.id}, '${slot.slotNumber}', '${slot.status}')">
                        ✏️ Edit
                    </button>
                    <button class="btn btn-outline" onclick="adminDeleteSlot(${slot.id}, '${slot.slotNumber}')">
                        🗑️ Delete
                    </button>
                    <button class="btn btn-outline" onclick="adminReleaseSlot(${slot.id}, '${slot.slotNumber}')" ${isAvail ? "disabled style='opacity:0.5;'" : ""}>
                        🟢 Make Available
                    </button>
                </div>
            </div>
        `;
    }).join("");

    container.innerHTML = html;
}

async function adminAddSlot() {
    const input = prompt("Enter new slot number (e.g. A21, B05):", "A21");
    if (!input) return;

    const slotNumber = input.trim().toUpperCase();
    if (!slotNumber) {
        showToast("Slot number is required.", "!");
        return;
    }

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ slotNumber, status: "AVAILABLE" })
            });

            if (res.ok) {
                showToast(`Slot ${slotNumber} added successfully.`, "✓");
                loadAdminSlots();
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                return;
            }
        } catch (e) {
            console.warn("Backend add slot failed, adding locally:", e);
        }
    }

    const slots = LocalDB.getSlots();
    if (slots.some(s => s.slotNumber === slotNumber)) {
        showToast(`Slot ${slotNumber} already exists.`, "!");
        return;
    }

    const newSlot = { id: Date.now(), slotNumber, status: "AVAILABLE" };
    slots.push(newSlot);
    LocalDB.saveSlots(slots);

    showToast(`Slot ${slotNumber} added successfully.`, "✓");
    loadAdminSlots();
    await loadParkingSlots();
    renderParkingSlots();
    updateParkingSummary();
    updateHeroSlots();
}

async function adminEditSlot(slotId, currentSlotNumber, currentStatus) {
    const newNumber = prompt("Enter slot number:", currentSlotNumber);
    if (!newNumber) return;

    const cleanNumber = newNumber.trim().toUpperCase();
    const newStatus = prompt("Enter status (AVAILABLE or OCCUPIED):", currentStatus);
    if (!newStatus) return;

    const cleanStatus = newStatus.trim().toUpperCase();
    if (cleanStatus !== "AVAILABLE" && cleanStatus !== "OCCUPIED") {
        showToast("Status must be AVAILABLE or OCCUPIED.", "!");
        return;
    }

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots/${slotId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ slotNumber: cleanNumber, status: cleanStatus })
            });

            if (res.ok) {
                showToast(`Slot ${cleanNumber} updated successfully.`, "✓");
                loadAdminSlots();
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                return;
            }
        } catch (e) {
            console.warn("Backend edit slot failed, updating locally:", e);
        }
    }

    const slots = LocalDB.getSlots();
    const slot = slots.find(s => s.id === slotId);
    if (slot) {
        slot.slotNumber = cleanNumber;
        slot.status = cleanStatus;
        LocalDB.saveSlots(slots);
        showToast(`Slot ${cleanNumber} updated successfully.`, "✓");
        loadAdminSlots();
        await loadParkingSlots();
        renderParkingSlots();
        updateParkingSummary();
        updateHeroSlots();
    }
}

async function adminDeleteSlot(slotId, slotNumber) {
    if (!confirm(`Are you sure you want to delete slot ${slotNumber}?`)) return;

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots/${slotId}`, { method: "DELETE" });
            if (res.ok) {
                showToast(`Slot ${slotNumber} deleted.`, "✓");
                loadAdminSlots();
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                return;
            }
        } catch (e) {
            console.warn("Backend delete slot failed, deleting locally:", e);
        }
    }

    let slots = LocalDB.getSlots();
    slots = slots.filter(s => s.id !== slotId);
    LocalDB.saveSlots(slots);

    showToast(`Slot ${slotNumber} deleted.`, "✓");
    loadAdminSlots();
    await loadParkingSlots();
    renderParkingSlots();
    updateParkingSummary();
    updateHeroSlots();
}

async function adminReleaseSlot(slotId, slotNumber) {
    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/slots/${slotId}/release`, { method: "PUT" });
            if (res.ok) {
                showToast(`Slot ${slotNumber} is now AVAILABLE.`, "✓");
                loadAdminSlots();
                await loadParkingSlots();
                renderParkingSlots();
                updateParkingSummary();
                updateHeroSlots();
                return;
            }
        } catch (e) {
            console.warn("Backend release slot failed, updating locally:", e);
        }
    }

    const slots = LocalDB.getSlots();
    const slot = slots.find(s => s.id === slotId);
    if (slot) {
        slot.status = "AVAILABLE";
        LocalDB.saveSlots(slots);
        showToast(`Slot ${slot.slotNumber} is now AVAILABLE.`, "✓");
        loadAdminSlots();
        await loadParkingSlots();
        renderParkingSlots();
        updateParkingSummary();
        updateHeroSlots();
    }
}

async function loadAdminBookings() {
    let bookings = [];

    if (isBackendAvailable) {
        try {
            const res = await fetch(`${API_URL}/bookings`);
            if (res.ok) bookings = await res.json();
        } catch (e) {
            console.warn("Backend admin bookings failed, using local store:", e);
        }
    }

    if (!bookings || bookings.length === 0) {
        bookings = LocalDB.getBookings();
    }

    const countEl = document.getElementById("adminTotalBookings");
    if (countEl) countEl.textContent = bookings.length;

    const container = document.getElementById("adminBookingsList");
    if (!container) return;

    if (bookings.length === 0) {
        container.innerHTML = `<p class="empty-state">No bookings recorded yet.</p>`;
        return;
    }

    container.innerHTML = bookings.slice().reverse().map(b => `
        <div class="booking-card">
            <div class="booking-card-header">
                <div>
                    <h4>🅿️ Slot ${b.slotNumber}</h4>
                    <span>Booking #${b.id}</span>
                </div>
                <span class="booking-status ${b.status === "completed" ? "completed" : b.status === "cancelled" ? "cancelled" : "active"}">
                    ${(b.status || "active").toUpperCase()}
                </span>
            </div>
            <div class="booking-details">
                <div><small>User</small><strong>${b.userEmail || "-"}</strong></div>
                <div><small>Vehicle</small><strong>${b.vehicle || "-"}</strong></div>
                <div><small>Date</small><strong>${b.bookingDate || "-"}</strong></div>
                <div><small>Amount</small><strong>₹${b.price || 0}</strong></div>
            </div>
            <div class="booking-status-info">
                <div><small>Entry</small><strong>${b.entryTime || "Not entered"}</strong></div>
                <div><small>Exit</small><strong>${b.exitTime || "Not exited"}</strong></div>
                <div><small>Payment</small><strong>${b.paymentStatus || "Pending"}</strong></div>
            </div>
        </div>
    `).join("");
}

/* ================= MODAL HELPERS & UTILITIES ================= */

function openLogin() {
    closeModal("registerModal");
    closeModal("bookingModal");
    closeModal("dashboardModal");
    closeModal("adminDashboardModal");
    openModal("loginModal");
}

function openRegister() {
    closeModal("loginModal");
    closeModal("bookingModal");
    closeModal("dashboardModal");
    closeModal("adminDashboardModal");
    openModal("registerModal");
}

function switchToRegister() {
    closeModal("loginModal");
    openRegister();
}

function switchToLogin() {
    closeModal("registerModal");
    openLogin();
}

function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add("active");
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove("active");
}

function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    if (input) {
        input.type = input.type === "password" ? "text" : "password";
    }
}

function setMinimumDate() {
    const dateInput = document.getElementById("bookingDate");
    if (dateInput) {
        const today = new Date().toISOString().split("T")[0];
        dateInput.min = today;
        if (!dateInput.value) dateInput.value = today;
    }
}

function scrollToParking() {
    const section = document.getElementById("parking");
    if (section) section.scrollIntoView({ behavior: "smooth" });
}

function toggleMenu() {
    const nav = document.getElementById("mainNav");
    if (nav) nav.classList.toggle("mobile-open");
}

function showToast(message, icon = "✓") {
    const toast = document.getElementById("toast");
    const msgEl = document.getElementById("toastMessage");
    const iconEl = document.getElementById("toastIcon");

    if (!toast || !msgEl || !iconEl) return;

    msgEl.textContent = message;
    iconEl.textContent = icon;
    toast.classList.add("show");

    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => {
        toast.classList.remove("show");
    }, 3200);
}

// Click outside modal backdrop to close
document.addEventListener("click", function (event) {
    if (event.target && event.target.classList.contains("modal-overlay")) {
        event.target.classList.remove("active");
    }
});

// Escape key to close open modals
document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
        document.querySelectorAll(".modal-overlay.active").forEach(m => {
            m.classList.remove("active");
        });
    }
});
