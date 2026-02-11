const chatHistory = document.getElementById('chat-history');
const messageInput = document.getElementById('chatInput');
const sendBtn = document.getElementById('sendBtn');

// Global selection state
let selectedItems = [];

// Handle Enter key
if (messageInput) {
    messageInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });
}

// Handle Send Button
if (sendBtn) {
    sendBtn.addEventListener('click', () => sendMessage());
}

function sendMessage(text = null) {
    const message = text || messageInput.value.trim();
    if (!message) return;

    // Add User Message
    addMessageToChat('user', message);
    if (!text) messageInput.value = '';

    // Simulate thinking
    const loadingId = addMessageToChat('bot', '...', true);

    // Disable input
    if (messageInput) messageInput.disabled = true;
    if (sendBtn) sendBtn.disabled = true;

    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message })
    })
        .then(response => response.json())
        .then(data => {
            // Remove loading
            const loadingEl = document.getElementById(loadingId);
            if (loadingEl) loadingEl.remove();

            // Process Response
            // API returns {status: "success", message: "..."} OR {success: true, data: "..."}
            // Let's handle both for safety
            const reply = data.data || data.message;

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
        // Render Menu Chips
        const categories = response.substring(9).split('|');
        let html = '<div>Here are our categories:</div><div class="menu-chips">';
        categories.forEach(cat => {
            if (cat.trim()) {
                html += `<div class="chip" onclick="sendMessage('Show items in ${cat}')">${cat}</div>`;
            }
        });
        html += '</div>';
        addMessageToChat('bot', html, false, true);

    } else if (response.includes('$$SELECTABLE$$:')) {
        // Render Selectable Cards
        const lines = response.split('\n');
        let html = '';
        let hasItems = false;

        lines.forEach(line => {
            if (line.includes('$$SELECTABLE$$:')) {
                // Format: $$SELECTABLE$$:ID|Name|Price|Seller|Rating|Speed
                const parts = line.replace('$$SELECTABLE$$:', '').split('|');
                if (parts.length >= 6) {
                    const id = parts[0];
                    const name = parts[1];
                    const price = parts[2];
                    const seller = parts[3];
                    const rating = parts[4];
                    const speed = parts[5];

                    html += `
                    <div class="product-card" onclick="toggleSelection(this, '${id}', '${name}', '${price}')">
                        <div class="product-info">
                            <span class="product-name">${name}</span>
                            <div class="product-meta">
                                <span class="product-price">${price}</span> • ⭐ ${rating} • 🚀 ${speed}
                            </div>
                            <div class="product-meta" style="color: #aaa;">${seller}</div>
                        </div>
                        <div class="product-check"></div>
                    </div>`;
                    hasItems = true;
                }
            } else {
                // Regular text
                if (line.trim()) html += `<div>${line}</div>`;
            }
        });

        if (hasItems) {
            html += `<div class="action-actions">
                        <button class="add-to-cart-btn" onclick="addSelectionToCart()">Add Selected to Order</button>
                     </div>`;
        }

        addMessageToChat('bot', html, false, true);

    } else {
        // Standard Text
        const formatted = response.replace(/\n/g, '<br>');
        addMessageToChat('bot', formatted, false, true);
    }
}

function toggleSelection(card, id, name, price) {
    card.classList.toggle('selected');

    // Check if selected
    const isSelected = card.classList.contains('selected');

    if (isSelected) {
        selectedItems.push({ id, name, price });
    } else {
        const index = selectedItems.findIndex(item => item.id === id);
        if (index > -1) {
            selectedItems.splice(index, 1);
        }
    }
}

function addSelectionToCart() {
    if (selectedItems.length === 0) {
        alert("Please select at least one item.");
        return;
    }

    // Format order data as JSON string
    const orderData = JSON.stringify(selectedItems);
    let msg = "ORDER:" + orderData;
    sendMessage(msg);

    // Clear selection state and remove visual selection
    document.querySelectorAll('.product-card.selected').forEach(card => {
        card.classList.remove('selected');
    });
    selectedItems = [];
}

function addMessageToChat(sender, text, isLoading = false, isHtml = false) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${sender}`;
    if (isLoading) msgDiv.id = 'loading-' + Date.now();

    const contentDiv = document.createElement('div');
    contentDiv.className = 'content';

    if (isHtml) {
        contentDiv.innerHTML = text;
    } else {
        contentDiv.textContent = text;
    }

    msgDiv.appendChild(contentDiv);

    if (chatHistory) {
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    return msgDiv.id;
}
