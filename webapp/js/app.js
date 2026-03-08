const chatHistory = document.getElementById('chat-history');
const messageInput = document.getElementById('chatInput');
const sendBtn = document.getElementById('sendBtn');

// Global state
let cart = [];
let hasPlacedOrder = false;

// UI Elements
const cartDrawer = document.getElementById('cartDrawer');
const cartCountBadge = document.getElementById('cartCountBadge');
const cartItemsContainer = document.getElementById('cartItemsContainer');
const cartTotalPrice = document.getElementById('cartTotalPrice');

// Enter key
if (messageInput) {
    messageInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') sendMessage();
    });
}

// Send Button
if (sendBtn) {
    sendBtn.addEventListener('click', () => sendMessage());
}

function sendMessage(text = null) {
    const message = text || messageInput.value.trim();
    if (!message) return;

    addMessageToChat('user', message);
    if (!text) messageInput.value = '';

    const loadingId = addMessageToChat('bot', '...', true);

    if (messageInput) messageInput.disabled = true;
    if (sendBtn) sendBtn.disabled = true;

    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message })
    })
        .then(response => response.json())
        .then(data => {
            const loadingEl = document.getElementById(loadingId);
            if (loadingEl) loadingEl.remove();

            let reply = data.data || data.message;
            if (typeof reply === 'string' && reply.startsWith('"') && reply.endsWith('"')) {
                reply = reply.substring(1, reply.length - 1);
                reply = reply.replace(/\\n/g, '\n').replace(/\\"/g, '"');
            }

            if (data.status === 'success' || data.success) {
                processBotResponse(reply);
            } else {
                addMessageToChat('bot', 'Error: ' + (data.message || 'Unknown error'));
            }
        })
        .catch(error => {
            console.error('Error:', error);
            const loadingEl = document.getElementById(loadingId);
            if (loadingEl) loadingEl.remove();
            addMessageToChat('bot', 'Connection error. Please try again.');
        })
        .finally(() => {
            if (messageInput) {
                messageInput.disabled = false;
                messageInput.focus();
            }
            if (sendBtn) sendBtn.disabled = false;
        });
}

function processBotResponse(response) {
    if (!response) return;

    if (response.startsWith('$$MENU$$:')) {
        const categories = response.substring(9).split('|');
        let html = '<div style="margin-bottom:8px;">Here are our categories:</div><div class="menu-chips">';
        categories.forEach(cat => {
            if (cat.trim()) {
                html += `<div class="chip" onclick="sendMessage('show items in ${cat}')">${cat}</div>`;
            }
        });
        html += '</div>';
        addMessageToChat('bot', html, false, true);

    } else if (response.includes('$$SELECTABLE$$:')) {
        const lines = response.split('\n');
        let html = '<div class="products-grid">';
        let hasItems = false;
        let textPrefix = '';

        lines.forEach(line => {
            if (line.includes('$$SELECTABLE$$:')) {
                const parts = line.replace('$$SELECTABLE$$:', '').split('|');
                if (parts.length >= 7) {
                    const id = parts[0];
                    const name = parts[1];
                    const price = parts[2];
                    const seller = parts[3];
                    const rating = parts[4];
                    const speed = parts[5];
                    const category = parts[6];

                    html += `
                    <div class="product-card">
                        <div class="product-info">
                            <div class="product-name">${name}</div>
                            <div class="product-price">${price}</div>
                            <div class="product-meta">⭐ ${rating} &nbsp;·&nbsp; ⏱ ${speed} &nbsp;·&nbsp; ${seller}</div>
                        </div>
                        <button class="add-btn" onclick="addToCart('${id}', '${name}', '${price}', '${seller}')">
                            + Add
                        </button>
                    </div>`;
                    hasItems = true;
                }
            } else if (line.trim()) {
                textPrefix += `<div>${line}</div>`;
            }
        });
        html += '</div>';

        if (hasItems) {
            addMessageToChat('bot', textPrefix + html, false, true);
        } else {
            addMessageToChat('bot', textPrefix.replace(/\n/g, '<br>'), false, true);
        }

    } else {
        const formatted = response.replace(/\n/g, '<br>');
        addMessageToChat('bot', formatted, false, true);
    }
}

function addMessageToChat(sender, text, isLoading = false, isHtml = false) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `msg ${sender}`;
    if (isLoading) msgDiv.id = 'loading-' + Date.now();

    if (isHtml) msgDiv.innerHTML = text;
    else msgDiv.textContent = text;

    if (chatHistory) {
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }
    return msgDiv.id;
}

// ── Shopping Cart Logic ──
function toggleCart() {
    if (cartDrawer) cartDrawer.classList.toggle('open');
}

function addToCart(id, name, priceStr, seller) {
    let numPrice = parseInt(priceStr.replace(/[^0-9]/g, ''), 10) || 0;
    cart.push({ id, name, price: numPrice.toString(), priceStr, seller });
    renderCart();

    // Quick pop effect on badge
    cartCountBadge.style.transform = 'scale(1.5)';
    setTimeout(() => cartCountBadge.style.transform = 'scale(1)', 200);

    if (!cartDrawer.classList.contains('open')) {
        toggleCart(); // Auto open cart on first add
    }
}

function removeFromCart(index) {
    cart.splice(index, 1);
    renderCart();
}

function renderCart() {
    const countBadge = document.getElementById('cartCountBadge');
    if (countBadge) countBadge.textContent = cart.length + (cart.length === 1 ? ' item' : ' items');

    if (cart.length === 0) {
        cartItemsContainer.innerHTML = '<div class="cart-empty">Your cart is empty.<br>Ask the assistant to find items!</div>';
        if (cartTotalPrice) cartTotalPrice.textContent = 'Rp 0';
        return;
    }

    let html = '';
    let total = 0;
    cart.forEach((item, index) => {
        total += parseInt(item.price, 10);
        html += `
        <div class="cart-item">
            <div class="cart-item-info">
                <span class="cart-item-name">${item.name}</span>
                <span class="cart-item-price">${item.priceStr}</span>
            </div>
            <button class="cart-item-remove" onclick="removeFromCart(${index})" title="Remove">&times;</button>
        </div>`;
    });

    cartItemsContainer.innerHTML = html;
    if (cartTotalPrice) cartTotalPrice.textContent = 'Rp ' + total.toLocaleString('id-ID');
}

// ── Checkout & Maps Logic ──
let orderNoteCache = "";
let mapInstance = null;
let mapMarker = null;
let currentLat = -6.200000;
let currentLng = 106.816666;

function proceedToCheckout() {
    if (cart.length === 0) {
        alert("Your cart is still empty!");
        return;
    }
    toggleCart(); // close drawer
    orderNoteCache = prompt("Additional note for seller (optional):", "") || "";
    document.getElementById('mapModalOverlay').style.display = 'block';
    document.getElementById('mapModal').style.display = 'block';

    if (!mapInstance) {
        mapInstance = L.map('map').setView([currentLat, currentLng], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OS'
        }).addTo(mapInstance);

        mapMarker = L.marker([currentLat, currentLng], { draggable: true }).addTo(mapInstance);
        mapMarker.on('dragend', function (event) {
            var position = mapMarker.getLatLng();
            currentLat = position.lat;
            currentLng = position.lng;
        });
        mapInstance.on('click', function (e) {
            mapMarker.setLatLng(e.latlng);
            currentLat = e.latlng.lat;
            currentLng = e.latlng.lng;
        });
    }
    setTimeout(() => { mapInstance.invalidateSize(); }, 200);
}

function closeMapModal() {
    document.getElementById('mapModalOverlay').style.display = 'none';
    document.getElementById('mapModal').style.display = 'none';
}

function submitOrderWithAddress() {
    const address = document.getElementById('buyerAddress').value.trim();
    if (!address) {
        alert("Please enter a detailed address.");
        return;
    }

    const orderData = JSON.stringify(cart);
    let msg = "ORDER:" + orderData;
    if (orderNoteCache) msg += "|NOTE:" + orderNoteCache;
    msg += "|ADDRESS:" + address + "|LAT:" + currentLat + "|LNG:" + currentLng;

    sendMessage(msg);

    hasPlacedOrder = true;
    document.getElementById('sellerChatBtn').style.display = 'flex';

    cart = [];
    renderCart();
    closeMapModal();
}

// ── Seller Chat Modal ──
function openSellerChatModal() {
    document.getElementById('sellerChatModalOverlay').style.display = 'block';
    document.getElementById('sellerChatModal').style.display = 'block';
    document.getElementById('sellerChatBtn').style.background = '#111';
}

function closeSellerChatModal() {
    document.getElementById('sellerChatModalOverlay').style.display = 'none';
    document.getElementById('sellerChatModal').style.display = 'none';
}

function escHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"'`=\/]/g, function (s) {
        return {
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;',
            "'": '&#39;', '/': '&#x2F;', '`': '&#x60;', '=': '&#x3D;'
        }[s];
    });
}

function addMessageToSellerChat(sender, text) {
    const history = document.getElementById('sellerChatHistory');
    const msgDiv = document.createElement('div');
    msgDiv.className = 'seller-msg-bubble';

    if (sender === 'Guest' || sender === 'Buyer') {
        msgDiv.classList.add('mine');
    } else {
        msgDiv.classList.add('theirs');
        text = `<b>${escHtml(sender)}:</b> ${text}`;
    }

    msgDiv.innerHTML = text.replace(/\n/g, '<br>');
    history.appendChild(msgDiv);
    history.scrollTop = history.scrollHeight;
}

function sendDirectMessageToSeller() {
    const input = document.getElementById('sellerChatInput');
    const text = input.value.trim();
    if (!text) return;

    addMessageToSellerChat('Guest', text);
    input.value = '';

    fetch('/api/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sender: "Guest", receiver: "Seller", content: text })
    }).catch(err => console.error('Failed to send message', err));
}

// ── Polling for Messages ──
setInterval(() => {
    fetch('/api/messages')
        .then(response => response.json())
        .then(data => {
            if ((data.status === 'success' || data.success) && data.data) {
                const messages = typeof data.data === 'string' ? JSON.parse(data.data) : data.data;
                messages.forEach(msg => {
                    hasPlacedOrder = true;
                    document.getElementById('sellerChatBtn').style.display = 'flex';

                    addMessageToSellerChat(msg.sender, msg.content);

                    if (document.getElementById('sellerChatModal').style.display === 'none') {
                        document.getElementById('sellerChatBtn').style.background = '#e74c3c';
                        setTimeout(() => {
                            document.getElementById('sellerChatBtn').style.background = '#111';
                        }, 3000);
                    }
                });
            }
        }).catch(err => { /* quiet drop */ });
}, 3000);
