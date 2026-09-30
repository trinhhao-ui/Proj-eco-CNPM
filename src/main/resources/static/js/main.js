/**
 * E-Commerce Main JavaScript Helper
 * Modern UI interactions, toast autohide, image preview, password toggle
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Auto-dismiss alerts after 4.5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            if (typeof bootstrap !== 'undefined' && bootstrap.Alert) {
                const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                bsAlert.close();
            } else {
                alert.style.transition = 'opacity 0.5s ease';
                alert.style.opacity = '0';
                setTimeout(() => alert.remove(), 500);
            }
        }, 4500);
    });

    // 2. Image preview when uploading product image
    const imageInput = document.querySelector('input[type="file"][name="image"]');
    if (imageInput) {
        imageInput.addEventListener('change', function (e) {
            const file = e.target.files[0];
            if (file) {
                let previewImg = document.getElementById('productImagePreview');
                if (!previewImg) {
                    previewImg = document.createElement('img');
                    previewImg.id = 'productImagePreview';
                    previewImg.className = 'rounded border shadow-sm mt-2';
                    previewImg.style.maxHeight = '150px';
                    previewImg.style.maxWidth = '100%';
                    previewImg.style.objectFit = 'contain';
                    imageInput.parentNode.appendChild(previewImg);
                }
                const reader = new FileReader();
                reader.onload = function (event) {
                    previewImg.src = event.target.result;
                };
                reader.readAsDataURL(file);
            }
        });
    }

    // 3. Password visibility toggle
    const togglePasswordBtns = document.querySelectorAll('.toggle-password-btn');
    togglePasswordBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const targetInput = document.getElementById(targetId);
            if (targetInput) {
                const isPassword = targetInput.type === 'password';
                targetInput.type = isPassword ? 'text' : 'password';
                const icon = this.querySelector('i');
                if (icon) {
                    icon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
                }
            }
        });
    });

    // 4. Quantity Stepper (+ / -) instant feedback
    const stepperMinus = document.querySelectorAll('.stepper-minus');
    const stepperPlus = document.querySelectorAll('.stepper-plus');

    stepperMinus.forEach(btn => {
        btn.addEventListener('click', function () {
            const input = this.parentNode.querySelector('input[type="number"]');
            if (input) {
                const min = parseInt(input.min) || 1;
                const current = parseInt(input.value) || 1;
                if (current > min) {
                    input.value = current - 1;
                }
            }
        });
    });

    stepperPlus.forEach(btn => {
        btn.addEventListener('click', function () {
            const input = this.parentNode.querySelector('input[type="number"]');
            if (input) {
                const max = parseInt(input.max) || 9999;
                const current = parseInt(input.value) || 1;
                if (current < max) {
                    input.value = current + 1;
                }
            }
        });
    });

    // 5. Real-time WebSocket Notifications via STOMP
    const navbar = document.querySelector('nav[data-user-id]');
    const currentUserId = navbar ? navbar.getAttribute('data-user-id') : null;

    if (currentUserId && currentUserId !== 'null' && typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
        try {
            const socket = new SockJS('/ws');
            const stompClient = Stomp.over(socket);
            stompClient.debug = null; // disable console logging spam

            stompClient.connect({}, function () {
                stompClient.subscribe('/topic/user/' + currentUserId, function (message) {
                    try {
                        const notification = JSON.parse(message.body);
                        showNotificationToast(notification.title || 'Thông báo mới', notification.message || '');
                    } catch (e) {
                        console.error('Error parsing notification', e);
                    }
                });
            }, function (error) {
                // Connection closed or failed, fail silently
            });
        } catch (e) {
            console.error('WebSocket connection error', e);
        }
    }

    function showNotificationToast(title, body) {
        let toastContainer = document.getElementById('globalToastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'globalToastContainer';
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        const toastEl = document.createElement('div');
        toastEl.className = 'toast align-items-center text-bg-primary border-0 show shadow-lg mb-2';
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');
        toastEl.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <div class="fw-bold"><i class="bi bi-bell-fill me-1"></i> ${escapeHtml(title)}</div>
                    <div class="small mt-1">${escapeHtml(body)}</div>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;
        toastContainer.appendChild(toastEl);

        setTimeout(() => {
            if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
                const bsToast = bootstrap.Toast.getOrCreateInstance(toastEl);
                bsToast.hide();
            } else {
                toastEl.remove();
            }
        }, 6000);
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, function (m) { return map[m]; });
    }
});
