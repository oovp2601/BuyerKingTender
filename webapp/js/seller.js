// ── State ────────────────────────────────────────────────────────────────
let allOrders = [];
let currentFilter = 'all';
let pollTimer = null;
let knownOrderIds = new Set();
let firstLoad = true;

// ── Boot ─────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    fetchOrders();
    pollTimer = setInterval(fetchOrders, 5000);
});

// ── Data fetching ─────────────────────────────────────────────────────────
function fetchOrders() {
    fetch('/api/orders')
        .then(r => r.json())
        .then(data => {
            if (data.status === 'success' || data.success) {
                // data.data is the JSON string of the orders array
                let orders = [];
                try {
                    orders = typeof data.data === 'string' ? JSON.parse(data.data) : data.data;
                } catch (e) {
                    console.error('Failed to parse orders:', e);
                }
                handleNewOrders(orders);
                allOrders = orders;
                updateStats(orders);
                renderOrders();
            }
        })
        .catch(err => {
            console.error('Poll error:', err);
            document.getElementById('liveStatus').textContent = 'Offline';
            document.getElementById('liveStatus').style.color = '#ef4444';
        });
}

function handleNewOrders(freshOrders) {
    if (firstLoad) {
        freshOrders.forEach(o => knownOrderIds.add(o.order_id));
        firstLoad = false;
        return;
    }
    // Detect genuinely new orders
    freshOrders.forEach(order => {
        if (!knownOrderIds.has(order.order_id)) {
            knownOrderIds.add(order.order_id);
            showToast(`🛒 New order #${order.order_id} from ${order.buyer_name}!`, 'info');
        }
    });
}

// ── Stats ─────────────────────────────────────────────────────────────────
function updateStats(orders) {
    const list = getCategoryFilteredOrders(orders);
    document.getElementById('statTotal').textContent = list.length;
    document.getElementById('statPending').textContent = list.filter(o => o.status === 'pending').length;
    document.getElementById('statAccepted').textContent = list.filter(o => o.status === 'accepted').length;
    document.getElementById('statRejected').textContent = list.filter(o => o.status === 'rejected').length;
}

// ── Filtering ─────────────────────────────────────────────────────────────
let currentCategory = 'all';

function setCategoryFilter(category, btn) {
    currentCategory = category;
    document.querySelectorAll('.cat-filter-btn').forEach(t => t.classList.remove('active'));
    if (btn) btn.classList.add('active');
    updateStats(allOrders);
    renderOrders();
}

function setFilter(filter, btn) {
    currentFilter = filter;
    document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderOrders();
}

function getCategoryFilteredOrders(orders) {
    if (currentCategory === 'all') return orders;
    return orders.filter(o => {
        const items = parseItems(o.items);
        return items.some(i => i.category === currentCategory);
    });
}

// ── Rendering ─────────────────────────────────────────────────────────────
function renderOrders() {
    const container = document.getElementById('ordersList');

    let baseOrders = getCategoryFilteredOrders(allOrders);

    const filtered = currentFilter === 'all'
        ? baseOrders
        : baseOrders.filter(o => o.status === currentFilter);

    document.getElementById('orderCount').textContent = filtered.length;

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon" style="font-size:3.5rem; margin-bottom:15px; color:rgba(255,255,255,0.1);"><i class="fa-solid fa-inbox"></i></div>
                <h3>No Orders Found</h3>
                <p>Incoming orders from buyers will appear here in real-time.</p>
            </div>`;
        return;
    }

    container.innerHTML = filtered.map(order => renderOrderCard(order)).join('');
}

function renderOrderCard(order) {
    const items = parseItems(order.items);
    const isPending = order.status === 'pending';
    const timeStr = formatTime(order.created_at);
    const totalFormatted = formatRupiah(order.total_price);
    const noteHtml = order.note && order.note.trim() ? `<div class="order-note"><i class="fa-solid fa-comment-dots"></i> ${escHtml(order.note)}</div>` : '';

    let addressHtml = '';
    if (order.buyer_address && order.buyer_address !== 'null') {
        const mapsLink = `https://www.google.com/maps/search/?api=1&query=${order.latitude},${order.longitude}`;
        addressHtml = `<div style="font-size:0.9rem; color:#a5b4fc; margin-top:6px;"><i class="fa-solid fa-location-dot"></i> ${escHtml(order.buyer_address)} <a href="${mapsLink}" target="_blank" style="color:var(--accent); text-decoration:none; margin-left: 5px; font-weight:600;">[View Map]</a></div>`;
    }

    const itemsHtml = items.map(item => `
        <div class="item-row">
            <span class="item-name">${escHtml(item.name)}</span>
            <span class="item-price">${escHtml(item.price)}</span>
        </div>`).join('');

    const actionButtons = isPending ? `
        <div class="action-buttons">
            <button class="action-btn btn-accept" onclick="updateStatus(${order.order_id}, 'accepted', this)"><i class="fa-solid fa-check"></i> Accept</button>
            <button class="action-btn btn-reject" onclick="updateStatus(${order.order_id}, 'rejected', this)"><i class="fa-solid fa-xmark"></i> Reject</button>
            <button class="action-btn btn-chat" onclick="openChatModal('${escHtml(order.buyer_name)}')" title="Chat with Buyer"><i class="fa-solid fa-comment-dots"></i></button>
        </div>` : `
        <div class="action-buttons">
            <button class="action-btn btn-accept" disabled><i class="fa-solid fa-check"></i> Accept</button>
            <button class="action-btn btn-reject" disabled><i class="fa-solid fa-xmark"></i> Reject</button>
            <button class="action-btn btn-chat" onclick="openChatModal('${escHtml(order.buyer_name)}')" title="Chat with Buyer"><i class="fa-solid fa-comment-dots"></i></button>
        </div>`;

    let badgeLabel = '';
    let icon = '';
    if (order.status === 'pending') { badgeLabel = 'Pending Review'; icon = '<i class="fa-solid fa-hourglass-half"></i>'; }
    else if (order.status === 'accepted') { badgeLabel = 'Accepted'; icon = '<i class="fa-solid fa-check-double"></i>'; }
    else { badgeLabel = 'Rejected'; icon = '<i class="fa-solid fa-ban"></i>'; }

    return `
        <div class="order-card ${order.status}" id="order-card-${order.order_id}">
            <div class="order-header">
                <div class="order-meta">
                    <div class="order-id">Order ID: #${order.order_id}</div>
                    <div class="order-buyer"><i class="fa-solid fa-circle-user" style="color:var(--muted)"></i> ${escHtml(order.buyer_name)}</div>
                    ${addressHtml}
                    ${noteHtml}
                    <div class="order-time"><i class="fa-solid fa-clock"></i> ${timeStr}</div>
                </div>
                <div class="status-badge ${order.status}">${icon} ${badgeLabel}</div>
            </div>
            <div class="items-list">${itemsHtml}</div>
            <div class="order-footer">
                <div class="order-total">Order Total: <span>${totalFormatted}</span></div>
                ${actionButtons}
            </div>
        </div>`;
}

// ── Status update ─────────────────────────────────────────────────────────
function updateStatus(orderId, status, btn) {
    // Disable both buttons immediately for feedback
    const card = document.getElementById(`order-card-${orderId}`);
    if (card) {
        card.querySelectorAll('button').forEach(b => b.disabled = true);
    }

    fetch('/api/orders/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId: String(orderId), status: status })
    })
        .then(r => r.json())
        .then(data => {
            if (data.status === 'success' || data.success) {
                // Update locally without waiting for next poll
                const order = allOrders.find(o => o.order_id === orderId);
                if (order) order.status = status;
                renderOrders();
                updateStats(allOrders);
                const emoji = status === 'accepted' ? '✅' : '❌';
                showToast(`${emoji} Order #${orderId} ${status}!`, status === 'accepted' ? 'success' : 'error');
            } else {
                showToast('Failed to update order.', 'error');
                if (card) card.querySelectorAll('button').forEach(b => b.disabled = false);
            }
        })
        .catch(() => {
            showToast('Connection error. Try again.', 'error');
            if (card) card.querySelectorAll('button').forEach(b => b.disabled = false);
        });
}

// ── Helpers ───────────────────────────────────────────────────────────────
function parseItems(items) {
    if (!items) return [];
    if (typeof items === 'string') {
        try { items = JSON.parse(items); } catch { return []; }
    }
    if (!Array.isArray(items)) return [];
    return items;
}

function formatRupiah(value) {
    const num = parseFloat(value) || 0;
    return 'Rp' + num.toLocaleString('id-ID', { maximumFractionDigits: 0 });
}

function formatTime(ts) {
    if (!ts) return '';
    const d = new Date(ts.replace(' ', 'T'));
    if (isNaN(d)) return ts;
    return d.toLocaleString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ── Toast ─────────────────────────────────────────────────────────────────
let toastTimer = null;
function showToast(msg, type = 'info') {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className = `show ${type}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.classList.remove('show'); }, 3500);
}

// ── Buyer Chat ─────────────────────────────────────────────────────────────
let currentChatBuyer = null;

function addMessageToChatHistory(sender, text) {
    const history = document.getElementById('sellerChatHistory');
    if (!history) return;

    const msgDiv = document.createElement('div');
    msgDiv.className = 'seller-msg-bubble';

    if (sender === "Seller") {
        msgDiv.classList.add('mine');
        msgDiv.innerHTML = text.replace(/\n/g, '<br>');
    } else {
        msgDiv.classList.add('theirs');
        msgDiv.innerHTML = `<strong>${escHtml(sender)}:</strong> ${text.replace(/\n/g, '<br>')}`;
    }

    history.appendChild(msgDiv);
    history.scrollTop = history.scrollHeight;
}

function openChatModal(buyerName) {
    currentChatBuyer = buyerName;
    document.getElementById('chatBuyerNameText').textContent = buyerName;
    document.getElementById('chatBuyerName').textContent = `To: ${buyerName}`;
    document.getElementById('chatMessage').value = '';

    // Clear history
    const history = document.getElementById('sellerChatHistory');
    if (history) {
        history.innerHTML = '<div style="text-align:center; color:#aaa; font-size:0.8rem; padding:10px;">Start of conversation</div>';
    }

    document.getElementById('chatModal').style.display = 'block';
    document.getElementById('modalOverlay').style.display = 'block';
    document.getElementById('chatMessage').focus();
}

function closeChatModal() {
    document.getElementById('chatModal').style.display = 'none';
    document.getElementById('modalOverlay').style.display = 'none';
}

function sendChatMessage() {
    const content = document.getElementById('chatMessage').value.trim();
    if (!content) return;

    // Set sender to current category if selected
    const sender = "Seller";
    // We send to Guest.

    // Optimistic UI
    addMessageToChatHistory(sender, content);
    document.getElementById('chatMessage').value = '';

    fetch('/api/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sender: sender, receiver: "Guest", content: content }) // Assume buyer is Guest
    })
        .then(r => r.json())
        .then(data => {
            if (data.status === 'success' || data.success) {
                // Success silently
            } else {
                showToast('Failed to send message.', 'error');
            }
        })
        .catch(() => showToast('Error sending message', 'error'));
}

// Poll for messages from Buyer
setInterval(() => {
    fetch('/api/messages?receiver=Seller')
        .then(response => response.json())
        .then(data => {
            if ((data.status === 'success' || data.success) && data.data) {
                const messages = typeof data.data === 'string' ? JSON.parse(data.data) : data.data;
                messages.forEach(msg => {
                    // Update chat history if modal is open.
                    if (document.getElementById('chatModal').style.display === 'block') {
                        addMessageToChatHistory(msg.sender, msg.content);
                    } else {
                        showToast(`New Message from ${msg.sender}`, 'info');
                    }
                });
            }
        })
        .catch(err => console.error('Error polling messages:', err));
}, 5000);
