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
    document.getElementById('statTotal').textContent = orders.length;
    document.getElementById('statPending').textContent = orders.filter(o => o.status === 'pending').length;
    document.getElementById('statAccepted').textContent = orders.filter(o => o.status === 'accepted').length;
    document.getElementById('statRejected').textContent = orders.filter(o => o.status === 'rejected').length;
}

// ── Filtering ─────────────────────────────────────────────────────────────
function setFilter(filter, btn) {
    currentFilter = filter;
    document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    renderOrders();
}

// ── Rendering ─────────────────────────────────────────────────────────────
function renderOrders() {
    const container = document.getElementById('ordersList');
    const filtered = currentFilter === 'all'
        ? allOrders
        : allOrders.filter(o => o.status === currentFilter);

    document.getElementById('orderCount').textContent = filtered.length;

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📭</div>
                <h3>No orders here yet</h3>
                <p>Orders from buyers will appear here in real time.</p>
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

    const itemsHtml = items.map(item => `
        <div class="item-row">
            <span class="item-name">${escHtml(item.name)}</span>
            <span class="item-price">${escHtml(item.price)}</span>
        </div>`).join('');

    const actionButtons = isPending ? `
        <div class="action-buttons">
            <button class="btn-accept" onclick="updateStatus(${order.order_id}, 'accepted', this)">✅ Accept</button>
            <button class="btn-reject" onclick="updateStatus(${order.order_id}, 'rejected', this)">❌ Reject</button>
        </div>` : `
        <div class="action-buttons">
            <button class="btn-accept" disabled>✅ Accept</button>
            <button class="btn-reject" disabled>❌ Reject</button>
        </div>`;

    const badgeLabel = order.status === 'pending' ? '⏳ Pending'
        : order.status === 'accepted' ? '✅ Accepted' : '❌ Rejected';

    return `
        <div class="order-card ${order.status}" id="order-card-${order.order_id}">
            <div class="order-header">
                <div class="order-meta">
                    <div class="order-id">Order #${order.order_id}</div>
                    <div class="order-buyer">👤 ${escHtml(order.buyer_name)}</div>
                    <div class="order-time">🕐 ${timeStr}</div>
                </div>
                <div class="status-badge ${order.status}">${badgeLabel}</div>
            </div>
            <div class="items-list">${itemsHtml}</div>
            <div class="order-footer">
                <div class="order-total">Total: <span>${totalFormatted}</span></div>
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
