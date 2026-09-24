document.addEventListener("DOMContentLoaded", function () {

    let currentUser = null;
    let cartItems = [];

    const CART_API = "cart";

    // --------------------------------------------------
    // TOAST
    // --------------------------------------------------

    function toast(message) {
        const container = document.querySelector(".toasts") || document.body;
        const div = document.createElement("div");
        div.className = "fernwick-toast";
        div.textContent = message;
        div.style.padding = "14px 20px";
        div.style.borderRadius = "8px";
        div.style.background = "#14251a";
        div.style.color = "#fff";
        div.style.fontSize = "14px";
        div.style.marginTop = "8px";
        div.style.boxShadow = "0 10px 30px -12px rgba(20,37,26,.4)";

        if (container === document.body) {
            div.style.position = "fixed";
            div.style.bottom = "25px";
            div.style.right = "25px";
            div.style.zIndex = "9999";
        }

        container.appendChild(div);

        setTimeout(function () {
            div.remove();
        }, 2600);
    }

    function money(amount) {
        return "£" + Number(amount || 0).toFixed(2);
    }

    async function readJson(response) {
        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }

    // --------------------------------------------------
    // AUTH
    // --------------------------------------------------

    async function loadCurrentUser() {
        try {
            const response = await fetch("current-user", { method: "GET", credentials: "same-origin" });
            const data = await readJson(response);
            currentUser = data && data.loggedIn ? data : null;
        } catch (error) {
            console.error("Unable to check login:", error);
            currentUser = null;
        }
        updateAuthUI();
    }

    function updateAuthUI() {
        const accountLink = document.getElementById("accountLink");
        if (accountLink) {
            if (currentUser) {
                accountLink.href = "orders.html";
                accountLink.setAttribute("aria-label", "Signed in as " + currentUser.name + " — view orders");
            } else {
                accountLink.href = "login.html";
                accountLink.setAttribute("aria-label", "Sign in");
            }
        }

        document.querySelectorAll("[data-nav-orders]").forEach(function (link) {
            link.hidden = !currentUser;
        });
    }

    async function login(email, password) {
        const formData = new URLSearchParams();
        formData.append("email", email);
        formData.append("password", password);

        const response = await fetch("login", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: formData.toString()
        });

        const data = await readJson(response);

        if (!response.ok || !data || data.error) {
            throw new Error((data && data.error) || "Unable to sign in.");
        }

        currentUser = data;
        updateAuthUI();
        return data;
    }

    async function register(name, email, password) {
        const formData = new URLSearchParams();
        formData.append("name", name);
        formData.append("email", email);
        formData.append("password", password);

        const response = await fetch("register", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: formData.toString()
        });

        const data = await readJson(response);

        if (!response.ok || !data || data.error) {
            throw new Error((data && data.error) || "Unable to create your account.");
        }

        currentUser = data;
        updateAuthUI();
        return data;
    }

    async function logout() {
        try {
            await fetch("logout", { method: "POST", credentials: "same-origin" });
        } catch (error) {
            console.error("Logout error:", error);
        }
        currentUser = null;
        cartItems = [];
        updateAuthUI();
        updateCartUI();
        toast("Signed out");
    }

    window.Fernwick_login = login;
    window.Fernwick_register = register;
    window.Fernwick_logout = logout;

    // --------------------------------------------------
    // GET CART
    // --------------------------------------------------

    async function loadCart() {
        if (!currentUser) {
            cartItems = [];
            updateCartUI();
            return;
        }

        try {
            const response = await fetch(CART_API + "?user_id=" + currentUser.id, {
                method: "GET",
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error("Unable to load cart");
            }

            cartItems = (await readJson(response)) || [];
            updateCartUI();

        } catch (error) {
            console.error("Cart loading error:", error);
        }
    }

    // --------------------------------------------------
    // ADD ITEM TO CART
    // --------------------------------------------------

    async function addItem(productId, quantity) {
        if (!currentUser) {
            toast("Please sign in to add items to your basket.");
            setTimeout(function () {
                window.location.href = "login.html";
            }, 800);
            return;
        }

        quantity = quantity || 1;

        try {
            const formData = new URLSearchParams();
            formData.append("product_id", productId);
            formData.append("quantity", quantity);

            const response = await fetch(CART_API, {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            });

            const data = await readJson(response);

            if (!response.ok) {
                throw new Error((data && data.error) || "Unable to add item");
            }

            toast("Added to your basket");
            await loadCart();
            openCartDrawer();

        } catch (error) {
            console.error("Add to cart error:", error);
            toast(error.message || "Unable to add item to basket.");
        }
    }

    // --------------------------------------------------
    // REMOVE ITEM FROM CART
    // --------------------------------------------------

    async function removeItem(cartId) {
        try {
            const response = await fetch(CART_API + "?id=" + cartId, {
                method: "DELETE",
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error("Unable to remove item");
            }

            toast("Item removed");
            await loadCart();

        } catch (error) {
            console.error("Remove cart error:", error);
            toast("Unable to remove item.");
        }
    }

    // --------------------------------------------------
    // PLACE ORDER (checkout)
    // --------------------------------------------------

    async function placeOrder() {
        if (!currentUser) {
            toast("Please sign in to check out.");
            setTimeout(function () {
                window.location.href = "login.html";
            }, 800);
            return;
        }

        if (cartItems.length === 0) {
            toast("Your basket is empty.");
            return;
        }

        try {
            const response = await fetch("order", { method: "POST", credentials: "same-origin" });
            const data = await readJson(response);

            if (!response.ok || !data || data.error) {
                throw new Error((data && data.error) || "Unable to place order.");
            }

            toast("Order #" + data.order_id + " placed — thank you!");
            await loadCart();
            closeCartDrawer();

            if (window.location.pathname.endsWith("cart.html")) {
                window.location.href = "orders.html";
            }

        } catch (error) {
            console.error("Checkout error:", error);
            toast(error.message || "Unable to place your order.");
        }
    }

    document.querySelectorAll("[data-checkout]").forEach(function (button) {
        button.addEventListener("click", placeOrder);
    });

    // --------------------------------------------------
    // UPDATE CART UI
    // --------------------------------------------------

    function updateCartUI() {
        const totalQuantity = cartItems.reduce(function (sum, item) {
            return sum + Number(item.quantity || 0);
        }, 0);

        const subtotal = cartItems.reduce(function (sum, item) {
            return sum + Number(item.price || 0) * Number(item.quantity || 0);
        }, 0);

        document.querySelectorAll("[data-cart-count]").forEach(function (element) {
            element.textContent = totalQuantity;
            element.hidden = totalQuantity === 0;
        });

        const delivery = cartItems.length === 0 || subtotal >= 40 ? 0 : 4.95;

        document.querySelectorAll("[data-cart-subtotal]").forEach(function (element) {
            element.textContent = money(subtotal);
        });

        document.querySelectorAll("[data-cart-delivery]").forEach(function (element) {
            element.textContent = delivery === 0 ? "Free" : money(delivery);
        });

        document.querySelectorAll("[data-cart-total]").forEach(function (element) {
            element.textContent = money(subtotal + delivery);
        });

        document.querySelectorAll("[data-cart-empty]").forEach(function (element) {
            element.hidden = cartItems.length > 0;
        });

        document.querySelectorAll("[data-cart-filled]").forEach(function (element) {
            element.hidden = cartItems.length === 0;
        });

        document.querySelectorAll("[data-cart-lines]").forEach(function (container) {
            renderCartLines(container);
        });
    }

    function renderCartLines(container) {
        container.innerHTML = "";

        cartItems.forEach(function (item) {
            const line = document.createElement("div");
            line.className = "cart-item";
            line.style.display = "flex";
            line.style.gap = "12px";
            line.style.alignItems = "center";

            line.innerHTML =
                '<img src="' + (item.image || "") + '" alt="' + escapeHtml(item.name || "") + '" ' +
                'style="width:64px;height:64px;object-fit:cover;border-radius:var(--r-md);flex:none" />' +
                '<div style="flex:1;min-width:0">' +
                '<h4 style="font-size:var(--text-sm);margin-bottom:2px">' + escapeHtml(item.name || "") + "</h4>" +
                '<p class="muted num" style="font-size:var(--text-sm)">' + money(item.price) + " × " + item.quantity + "</p>" +
                "</div>" +
                '<button type="button" class="btn btn--icon btn--sm remove-cart-item" data-cart-id="' + item.id + '" aria-label="Remove ' + escapeHtml(item.name || "") + '">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>' +
                "</button>";

            container.appendChild(line);
        });

        container.querySelectorAll(".remove-cart-item").forEach(function (button) {
            button.addEventListener("click", function () {
                removeItem(button.getAttribute("data-cart-id"));
            });
        });
    }

    function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = value;
        return div.innerHTML;
    }

    // --------------------------------------------------
    // CART DRAWER
    // --------------------------------------------------

    function openCartDrawer() {
        const drawer = document.querySelector(".drawer");
        const scrim = document.getElementById("drawerScrim");
        if (drawer) drawer.setAttribute("data-open", "true");
        if (scrim) scrim.setAttribute("data-open", "true");
    }

    function closeCartDrawer() {
        const drawer = document.querySelector(".drawer");
        const scrim = document.getElementById("drawerScrim");
        if (drawer) drawer.setAttribute("data-open", "false");
        if (scrim) scrim.setAttribute("data-open", "false");
    }

    const scrim = document.getElementById("drawerScrim");
    if (scrim) {
        scrim.addEventListener("click", closeCartDrawer);
    }

    // --------------------------------------------------
    // GLOBAL CLICK HANDLING (add to basket, drawer, wishlist)
    // --------------------------------------------------

    document.addEventListener("click", function (event) {

        const addButton = event.target.closest("[data-add]");
        if (addButton) {
            event.preventDefault();
            const productId = addButton.getAttribute("data-add");
            const qtyInput = document.querySelector("[data-qty-input]");
            let quantity = 1;
            if (qtyInput && document.querySelector("[data-product]")) {
                quantity = Number(qtyInput.value) || 1;
            }
            addItem(productId, quantity);
            return;
        }

        const openDrawerButton = event.target.closest('[data-action="open-drawer"]');
        if (openDrawerButton) {
            event.preventDefault();
            openCartDrawer();
            return;
        }

        const closeDrawerButton = event.target.closest('[data-action="close-drawer"]');
        if (closeDrawerButton) {
            event.preventDefault();
            closeCartDrawer();
            return;
        }

        const toastButton = event.target.closest("[data-toast]");
        if (toastButton) {
            event.preventDefault();
            toast(toastButton.getAttribute("data-toast"));
            return;
        }

        const wishlistButton = event.target.closest(".product__wish");
        if (wishlistButton) {
            event.preventDefault();
            wishlistButton.classList.toggle("is-active");
            wishlistButton.setAttribute("aria-pressed", wishlistButton.classList.contains("is-active") ? "true" : "false");
            toast(wishlistButton.classList.contains("is-active") ? "Added to wishlist" : "Removed from wishlist");
            return;
        }

        const burger = event.target.closest(".burger");
        if (burger) {
            event.preventDefault();
            const nav = document.querySelector(".shopnav");
            if (nav) {
                const open = nav.getAttribute("data-open") === "true";
                nav.setAttribute("data-open", open ? "false" : "true");
                burger.setAttribute("aria-expanded", open ? "false" : "true");
            }
            return;
        }
    });

    // --------------------------------------------------
    // QUANTITY STEPPER (product page)
    // --------------------------------------------------

    document.querySelectorAll("[data-qty]").forEach(function (qty) {
        const input = qty.querySelector("[data-qty-input]");
        const decrease = qty.querySelector("[data-qty-decrease]") || qty.querySelector("button:first-of-type");
        const increase = qty.querySelector("[data-qty-increase]") || qty.querySelector("button:last-of-type");

        if (!input) return;

        if (decrease) {
            decrease.addEventListener("click", function () {
                const value = Math.max(1, (Number(input.value) || 1) - 1);
                input.value = value;
            });
        }

        if (increase) {
            increase.addEventListener("click", function () {
                const value = (Number(input.value) || 1) + 1;
                input.value = value;
            });
        }
    });

    // --------------------------------------------------
    // DEMO FORMS (newsletter, contact, promo code)
    // --------------------------------------------------

    document.querySelectorAll("[data-demo-form]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            toast(form.getAttribute("data-demo-form"));
            form.reset();
        });
    });

    // --------------------------------------------------
    // PRODUCT FILTERS + SORT + SEARCH (shop page)
    // --------------------------------------------------

    const productGrid = document.querySelector("[data-product-grid]");

    if (productGrid) {
        const filterButtons = document.querySelectorAll("[data-filter]");
        const productCards = Array.from(productGrid.querySelectorAll("[data-product]"));
        const sortSelect = document.querySelector("[data-sort]");
        const resultCount = document.querySelector("[data-result-count]");
        const noResults = document.querySelector("[data-no-results]");
        const searchInput = document.querySelector(".searchbox .input");
        const clearButtons = document.querySelectorAll("[data-clear-filters]");

        function activeFilters() {
            return Array.from(filterButtons)
                .filter(function (button) { return button.getAttribute("aria-pressed") === "true"; })
                .map(function (button) { return button.getAttribute("data-filter"); });
        }

        function applyFilters() {
            const filters = activeFilters();
            const query = (searchInput && searchInput.value || "").trim().toLowerCase();
            let visibleCount = 0;

            productCards.forEach(function (card) {
                const tags = (card.getAttribute("data-tags") || "").split(" ");
                const name = (card.getAttribute("data-name") || "").toLowerCase();

                const matchesFilters = filters.every(function (filter) { return tags.indexOf(filter) !== -1; });
                const matchesQuery = !query || name.indexOf(query) !== -1;

                const visible = matchesFilters && matchesQuery;
                card.style.display = visible ? "" : "none";
                if (visible) visibleCount++;
            });

            if (resultCount) resultCount.textContent = visibleCount;
            if (noResults) noResults.hidden = visibleCount !== 0;
        }

        function applySort() {
            if (!sortSelect) return;
            const value = sortSelect.value;
            const sorted = productCards.slice().sort(function (a, b) {
                if (value === "price-asc") return Number(a.getAttribute("data-price")) - Number(b.getAttribute("data-price"));
                if (value === "price-desc") return Number(b.getAttribute("data-price")) - Number(a.getAttribute("data-price"));
                if (value === "name") return (a.getAttribute("data-name") || "").localeCompare(b.getAttribute("data-name") || "");
                return Number(a.getAttribute("data-order")) - Number(b.getAttribute("data-order"));
            });
            sorted.forEach(function (card) { productGrid.appendChild(card); });
        }

        filterButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                const pressed = button.getAttribute("aria-pressed") === "true";
                button.setAttribute("aria-pressed", pressed ? "false" : "true");
                button.classList.toggle("is-active", !pressed);
                applyFilters();
            });
        });

        if (sortSelect) {
            sortSelect.addEventListener("change", function () {
                applySort();
                applyFilters();
            });
        }

        if (searchInput) {
            searchInput.addEventListener("input", applyFilters);
        }

        clearButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                filterButtons.forEach(function (filterButton) {
                    filterButton.setAttribute("aria-pressed", "false");
                    filterButton.classList.remove("is-active");
                });
                if (searchInput) searchInput.value = "";
                applyFilters();
            });
        });

        applyFilters();
    }

    // --------------------------------------------------
    // GALLERY (product page)
    // --------------------------------------------------

    const galleryMain = document.querySelector("[data-gallery-main]");
    const galleryThumbs = document.querySelectorAll("[data-gallery-thumb]");

    galleryThumbs.forEach(function (thumb) {
        thumb.addEventListener("click", function () {
            const image = thumb.getAttribute("data-gallery-thumb");
            if (galleryMain) galleryMain.src = image;
            galleryThumbs.forEach(function (item) { item.setAttribute("aria-current", "false"); });
            thumb.setAttribute("aria-current", "true");
        });
    });

    // --------------------------------------------------
    // PRODUCT VARIANT (pot size) SELECTOR
    // --------------------------------------------------

    document.querySelectorAll("[data-variant]").forEach(function (variant) {
        variant.addEventListener("click", function () {
            document.querySelectorAll("[data-variant]").forEach(function (item) {
                item.setAttribute("aria-pressed", "false");
            });
            variant.setAttribute("aria-pressed", "true");

            const price = Number(variant.getAttribute("data-price")) / 100;
            const priceElement = document.querySelector("[data-variant-price]");
            if (priceElement) priceElement.textContent = money(price);

            const addButton = document.getElementById("pdpAddButton");
            if (addButton) {
                addButton.setAttribute("data-price", variant.getAttribute("data-price"));
            }
        });
    });

    // --------------------------------------------------
    // REVEAL ANIMATION
    // --------------------------------------------------

    const revealElements = document.querySelectorAll("[data-reveal]");

    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        revealElements.forEach(function (element) { observer.observe(element); });
    } else {
        revealElements.forEach(function (element) { element.classList.add("is-visible"); });
    }

    // --------------------------------------------------
    // DYNAMIC PRODUCT DETAIL PAGE (?id=slug)
    // --------------------------------------------------

    async function hydrateProductPage() {
        const root = document.getElementById("pdpRoot");
        if (!root) return;

        const params = new URLSearchParams(window.location.search);
        const id = params.get("id");
        if (!id) return;

        try {
            const response = await fetch("products?id=" + encodeURIComponent(id), { credentials: "same-origin" });
            const product = await readJson(response);

            if (!response.ok || !product || product.error) {
                return;
            }

            document.title = product.name + " — Fernwick";
            setText("pdpBreadcrumb", product.name);
            setText("pdpTitle", product.name);
            setText("pdpLatin", product.latinName || "");
            setText("pdpDescription", "Grown at our nursery in the Lea Valley. " +
                (product.light ? "Likes " + product.light.toLowerCase() + " light. " : "") +
                (product.water ? "Water " + product.water.toLowerCase() + "." : ""));
            setText("pdpLight", product.light || "");
            setText("pdpWater", product.water || "");

            const priceElement = document.querySelector("[data-variant-price]");
            if (priceElement) priceElement.textContent = money(product.price);

            const wasElement = document.getElementById("pdpPriceWas");
            const saveBadge = document.getElementById("pdpSaveBadge");
            const priceWrap = document.getElementById("pdpPriceWrap");
            if (product.wasPrice) {
                if (wasElement) { wasElement.hidden = false; wasElement.textContent = money(product.wasPrice); }
                if (saveBadge) {
                    saveBadge.hidden = false;
                    const percent = Math.round((1 - product.price / product.wasPrice) * 100);
                    saveBadge.textContent = "Save " + percent + "%";
                }
                if (priceWrap) priceWrap.classList.add("price--sale");
            } else {
                if (wasElement) wasElement.hidden = true;
                if (saveBadge) saveBadge.hidden = true;
                if (priceWrap) priceWrap.classList.remove("price--sale");
            }

            const saleBadge = document.getElementById("pdpSaleBadge");
            if (saleBadge) {
                if (product.badge) { saleBadge.hidden = false; saleBadge.textContent = product.badge; }
                else { saleBadge.hidden = true; }
            }

            const stockBadge = document.getElementById("pdpStockBadge");
            if (stockBadge) {
                if (product.stock <= 0) {
                    stockBadge.innerHTML = '<span class="dot"></span>Out of stock';
                } else if (product.stock <= 5) {
                    stockBadge.innerHTML = '<span class="dot"></span>Low stock — only ' + product.stock + ' left';
                } else {
                    stockBadge.innerHTML = '<span class="dot"></span>In stock — ships Tuesday';
                }
            }

            const mainImage = document.querySelector("[data-gallery-main]");
            if (mainImage) mainImage.src = product.image;

            const thumbs = document.getElementById("pdpThumbs");
            if (thumbs) thumbs.hidden = true;

            const variants = document.getElementById("pdpVariants");
            if (variants) variants.hidden = true;

            const addButton = document.getElementById("pdpAddButton");
            if (addButton) {
                addButton.setAttribute("data-add", product.id);
                addButton.setAttribute("data-name", product.name);
                addButton.setAttribute("data-price", Math.round(product.price * 100));
                addButton.setAttribute("data-image", product.image);
                addButton.disabled = product.stock <= 0;
                addButton.textContent = product.stock <= 0 ? "Out of stock" : "Add to basket";
            }

        } catch (error) {
            console.error("Unable to load product:", error);
        }
    }

    function setText(id, text) {
        const element = document.getElementById(id);
        if (element) element.textContent = text;
    }

    hydrateProductPage();

    // --------------------------------------------------
    // AUTH FORMS (login / register)
    // --------------------------------------------------

    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        loginForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            const errorElement = document.getElementById("loginError");
            if (errorElement) errorElement.hidden = true;

            try {
                await login(
                    document.getElementById("loginEmail").value,
                    document.getElementById("loginPassword").value
                );
                toast("Welcome back!");
                window.location.href = "shop.html";
            } catch (error) {
                if (errorElement) {
                    errorElement.textContent = error.message;
                    errorElement.hidden = false;
                }
            }
        });
    }

    const registerForm = document.getElementById("registerForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            const errorElement = document.getElementById("registerError");
            if (errorElement) errorElement.hidden = true;

            try {
                await register(
                    document.getElementById("registerName").value,
                    document.getElementById("registerEmail").value,
                    document.getElementById("registerPassword").value
                );
                toast("Account created — welcome!");
                window.location.href = "shop.html";
            } catch (error) {
                if (errorElement) {
                    errorElement.textContent = error.message;
                    errorElement.hidden = false;
                }
            }
        });
    }

    // --------------------------------------------------
    // ORDERS PAGE
    // --------------------------------------------------

    async function loadOrdersPage() {
        const list = document.getElementById("ordersList");
        if (!list) return;

        const signedOut = document.getElementById("ordersSignedOut");
        const empty = document.getElementById("ordersEmpty");

        if (!currentUser) {
            if (signedOut) signedOut.hidden = false;
            return;
        }

        try {
            const response = await fetch("order", { credentials: "same-origin" });
            const orders = (await readJson(response)) || [];

            if (orders.length === 0) {
                if (empty) empty.hidden = false;
                return;
            }

            orders.forEach(function (order) {
                const card = document.createElement("div");
                card.style.background = "var(--paper)";
                card.style.border = "1px solid var(--hairline)";
                card.style.borderRadius = "var(--r-lg)";
                card.style.padding = "var(--s-5)";

                const items = (order.items || []).map(function (item) {
                    return escapeHtml(item.name) + " × " + item.quantity;
                }).join(", ");

                card.innerHTML =
                    '<div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:var(--s-3);margin-bottom:var(--s-3)">' +
                    "<div><h3 style=\"font-size:var(--text-md)\">Order #" + order.id + "</h3>" +
                    '<p class="muted label">' + escapeHtml(order.order_date) + "</p></div>" +
                    '<div style="text-align:right"><span class="badge badge--stock">' + escapeHtml(order.status) + "</span>" +
                    '<p class="num" style="margin-top:6px;font-weight:600">' + money(order.total_amount) + "</p></div>" +
                    "</div>" +
                    '<p class="muted" style="font-size:var(--text-sm)">' + items + "</p>";

                list.appendChild(card);
            });

        } catch (error) {
            console.error("Unable to load orders:", error);
        }
    }

    // --------------------------------------------------
    // INITIALIZE
    // --------------------------------------------------

    async function initialize() {
        await loadCurrentUser();
        await loadCart();
        await loadOrdersPage();
    }

    initialize();

});
