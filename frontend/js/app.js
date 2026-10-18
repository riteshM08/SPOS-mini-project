
const App = {
    state: {
        airports: [],
        searchResults: [],
        selectedFlight: null,
        selectedSeats: [],
        passengerCount: 1,
        passengersData: [],
        lastBooking: null,
        activeAdminTab: 'flights',
        trackerFlightId: null
    },

    async init() {
        this.bindEvents();
        this.updateAuthUI();
        await this.loadAirports();

        const todayStr = new Date().toISOString().split('T')[0];
        const dateInput = document.getElementById('search-dep-date');
        if (dateInput) {
            dateInput.value = todayStr;
            dateInput.min = todayStr;
        }

        const hash = window.location.hash.replace('#', '') || 'home';
        this.navigate(hash);
    },

    navigate(viewId) {
        document.querySelectorAll('.view-section').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.nav-links a').forEach(el => el.classList.remove('active'));

        const targetView = document.getElementById(`view-${viewId}`);
        if (targetView) {
            targetView.classList.add('active');
            window.location.hash = viewId;
        }

        const navLink = document.querySelector(`.nav-links a[data-navigate="${viewId}"]`);
        if (navLink) navLink.classList.add('active');

        if (viewId === 'my-bookings') {
            this.loadMyBookings();
        } else if (viewId === 'admin') {
            if (!API.isAdmin()) {
                this.showToast('Administrator privileges required.', 'error');
                this.openModal('modal-admin-login');
                this.navigate('home');
                return;
            }
            this.loadAdminDashboard();
        }
    },

    bindEvents() {
        document.querySelectorAll('[data-navigate]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                const view = el.getAttribute('data-navigate');
                this.navigate(view);
            });
        });

        const searchForm = document.getElementById('flight-search-form');
        if (searchForm) {
            searchForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleFlightSearch();
            });
        }

        const swapBtn = document.getElementById('btn-swap-airports');
        if (swapBtn) {
            swapBtn.addEventListener('click', () => {
                const src = document.getElementById('search-source');
                const dst = document.getElementById('search-dest');
                const tmp = src.value;
                src.value = dst.value;
                dst.value = tmp;
            });
        }

        const paxSelect = document.getElementById('search-passengers');
        if (paxSelect) {
            paxSelect.addEventListener('change', () => {
                this.state.passengerCount = parseInt(paxSelect.value, 10) || 1;
            });
        }

        const passengerLoginForm = document.getElementById('form-passenger-login');
        if (passengerLoginForm) {
            passengerLoginForm.addEventListener('submit', (e) => this.handleLogin(e, 'passenger', 'passenger'));
        }

        const adminLoginForm = document.getElementById('form-admin-login');
        if (adminLoginForm) {
            adminLoginForm.addEventListener('submit', (e) => this.handleLogin(e, 'admin', 'admin'));
        }

        const regForm = document.getElementById('form-register');
        if (regForm) {
            regForm.addEventListener('submit', (e) => this.handleRegister(e));
        }

        document.querySelectorAll('.modal-close-btn, .modal-backdrop').forEach(el => {
            el.addEventListener('click', (e) => {
                if (e.target === el) {
                    this.closeModals();
                }
            });
        });

        document.querySelectorAll('.admin-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.admin-tab-btn').forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.admin-tab-panel').forEach(p => p.style.display = 'none');

                btn.classList.add('active');
                const tab = btn.getAttribute('data-tab');
                this.state.activeAdminTab = tab;
                const panel = document.getElementById(`admin-panel-${tab}`);
                if (panel) panel.style.display = 'block';

                if (tab === 'flights') this.loadAdminFlights();
                if (tab === 'bookings') this.loadAdminBookings();
                if (tab === 'tracker') this.loadSeatTrackerOptions();
                if (tab === 'scheduling') this.loadSchedulingSimulation();
            });
        });
    },

    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div>${message}</div>
            <span style="cursor:pointer; margin-left:12px; font-weight:bold;">&times;</span>
        `;
        toast.querySelector('span').addEventListener('click', () => toast.remove());
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    openModal(modalId) {
        this.closeModals();
        const m = document.getElementById(modalId);
        if (m) m.classList.add('active');
    },

    closeModals() {
        document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('active'));
    },

    updateAuthUI() {
        const user = API.getCurrentUser();
        const guestGroup = document.getElementById('nav-guest-group');
        const userGroup = document.getElementById('nav-user-group');
        const adminLink = document.getElementById('nav-admin-link');

        if (user && API.isAuthenticated()) {
            if (guestGroup) guestGroup.style.display = 'none';
            if (userGroup) {
                userGroup.style.display = 'flex';
                document.getElementById('nav-user-name').textContent = user.name;
                document.getElementById('nav-user-avatar').textContent = user.name.charAt(0).toUpperCase();

                const rolePill = document.getElementById('nav-user-role');
                rolePill.textContent = user.role;
                rolePill.className = `role-pill role-${user.role}`;
            }
            if (adminLink) {
                adminLink.style.display = (user.role === 'admin') ? 'inline-block' : 'none';
            }
        } else {
            if (guestGroup) guestGroup.style.display = 'flex';
            if (userGroup) userGroup.style.display = 'none';
            if (adminLink) adminLink.style.display = 'none';
        }
    },

    async handleLogin(e, requiredRole, loginType) {
        e.preventDefault();
        const prefix = loginType === 'admin' ? 'admin-login' : 'passenger-login';
        const email = document.getElementById(`${prefix}-email`).value;
        const password = document.getElementById(`${prefix}-password`).value;
        const submitBtn = e.target.querySelector('button[type="submit"]');
        const originalButtonText = loginType === 'admin' ? 'Log In as Admin' : 'Log In as Passenger';

        submitBtn.disabled = true;
        submitBtn.textContent = 'Authenticating...';

        try {
            const res = await API.login(email, password, requiredRole);
            this.showToast(`Welcome back, ${res.user.name}!`, 'success');
            this.closeModals();
            this.updateAuthUI();

            if (res.user.role === 'admin') {
                this.navigate('admin');
            } else {
                if (window.location.hash === '#my-bookings') {
                    this.loadMyBookings();
                }
            }
        } catch (err) {
            this.showToast(err.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = originalButtonText;
        }
    },

    async handleRegister(e) {
        e.preventDefault();
        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;
        const password = document.getElementById('reg-password').value;
        const phone = document.getElementById('reg-phone').value;
        const submitBtn = e.target.querySelector('button[type="submit"]');

        submitBtn.disabled = true;
        submitBtn.textContent = 'Creating Account...';

        try {
            const res = await API.register(name, email, password, phone);
            this.showToast(`Account created successfully! Welcome, ${res.user.name}.`, 'success');
            this.closeModals();
            this.updateAuthUI();
        } catch (err) {
            this.showToast(err.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Create Passenger Account';
        }
    },

    async logout() {
        await API.logout();
        this.updateAuthUI();
        this.showToast('You have been logged out.', 'info');
        this.navigate('home');
    },

    async loadAirports() {
        try {
            const res = await API.getAirports();
            this.state.airports = res.airports || [];

            const srcSelect = document.getElementById('search-source');
            const dstSelect = document.getElementById('search-dest');
            const adminSrc = document.getElementById('admin-flight-source');
            const adminDst = document.getElementById('admin-flight-dest');

            const optionsHtml = this.state.airports.map(a =>
                `<option value="${a.airport_code}">${a.city} (${a.airport_code}) - ${a.airport_name}</option>`
            ).join('');

            const optionsWithEmpty = '<option value="">Select Airport</option>' + optionsHtml;

            if (srcSelect) {
                srcSelect.innerHTML = optionsWithEmpty;
                srcSelect.value = 'DEL'; // default Delhi
            }
            if (dstSelect) {
                dstSelect.innerHTML = optionsWithEmpty;
                dstSelect.value = 'BOM'; // default Mumbai
            }
            if (adminSrc) adminSrc.innerHTML = optionsHtml;
            if (adminDst) adminDst.innerHTML = optionsHtml;

        } catch (err) {
            console.error('Failed to load airports', err);
        }
    },

    async handleFlightSearch() {
        const src = document.getElementById('search-source').value;
        const dst = document.getElementById('search-dest').value;
        const date = document.getElementById('search-dep-date').value;
        const pax = parseInt(document.getElementById('search-passengers').value, 10) || 1;

        if (!src || !dst) {
            this.showToast('Please select both Origin and Destination airports.', 'warning');
            return;
        }
        if (src === dst) {
            this.showToast('Origin and Destination airports cannot be the same.', 'warning');
            return;
        }

        this.state.passengerCount = pax;
        const resultsContainer = document.getElementById('search-results-list');
        const emptyState = document.getElementById('search-empty-state');
        const loadingState = document.getElementById('search-loading-state');

        resultsContainer.innerHTML = '';
        emptyState.style.display = 'none';
        loadingState.style.display = 'block';

        this.navigate('search-results');

        try {
            const res = await API.searchFlights(src, dst, date, pax);
            this.state.searchResults = res.flights || [];

            loadingState.style.display = 'none';

            if (this.state.searchResults.length === 0) {
                emptyState.style.display = 'block';
                document.getElementById('search-route-summary').textContent = `${src} → ${dst} (${date || 'Any Date'})`;
                return;
            }

            document.getElementById('search-summary-title').textContent =
                `${this.state.searchResults[0].source_city} (${src}) to ${this.state.searchResults[0].dest_city} (${dst})`;
            document.getElementById('search-summary-sub').textContent =
                `${this.state.searchResults.length} flight(s) available for ${pax} passenger(s) on ${date || 'all dates'}`;

            this.renderFlightCards(this.state.searchResults);

        } catch (err) {
            loadingState.style.display = 'none';
            this.showToast('Failed to fetch flights: ' + err.message, 'error');
        }
    },

    renderFlightCards(flights) {
        const container = document.getElementById('search-results-list');
        container.innerHTML = '';

        flights.forEach(f => {
            const card = document.createElement('div');
            card.className = 'flight-card';

            const depTime = f.departure_time ? f.departure_time.split(' ')[1].substring(0, 5) : '00:00';
            const arrTime = f.arrival_time ? f.arrival_time.split(' ')[1].substring(0, 5) : '00:00';
            const hours = Math.floor(f.duration_minutes / 60);
            const mins = f.duration_minutes % 60;

            card.innerHTML = `
                <div class="flight-airline">
                    <div class="airline-badge">✈</div>
                    <div>
                        <div class="airline-name">${f.airline_name}</div>
                        <div class="flight-num">${f.flight_number} &bull; ${f.aircraft_model || 'Boeing 737'}</div>
                    </div>
                </div>

                <div class="flight-route-timeline">
                    <div class="route-point">
                        <div class="point-time">${depTime}</div>
                        <div class="point-code">${f.source_code}</div>
                        <div class="point-city">${f.source_city}</div>
                    </div>
                    <div class="route-connector">
                        <span class="duration-label">${hours}h ${mins}m</span>
                        <div class="timeline-line">
                            <span class="timeline-plane-icon">✈</span>
                        </div>
                        <span class="nonstop-badge">Non-stop</span>
                    </div>
                    <div class="route-point">
                        <div class="point-time">${arrTime}</div>
                        <div class="point-code">${f.dest_code}</div>
                        <div class="point-city">${f.dest_city}</div>
                    </div>
                </div>

                <div class="flight-fare-wrap">
                    <div class="fare-label">Starting from</div>
                    <div class="fare-amount">₹${f.fare.toLocaleString('en-IN')}</div>
                    <div class="seats-remaining">${f.available_seats} seats left</div>
                </div>

                <div>
                    <button class="btn btn-primary" onclick="App.startBookingFlow(${f.flight_id})">
                        Select Flight &rarr;
                    </button>
                </div>
            `;
            container.appendChild(card);
        });
    },

    async startBookingFlow(flightId) {
        if (!API.isAuthenticated()) {
            this.showToast('Please log in or register to book a ticket.', 'info');
            this.openModal('modal-passenger-login');
            return;
        }

        const flight = this.state.searchResults.find(f => f.flight_id === flightId);
        if (flight) {
            this.state.selectedFlight = flight;
        } else {
            try {
                const res = await API.getFlightDetails(flightId);
                this.state.selectedFlight = res.flight;
            } catch (e) {
                this.showToast('Could not load flight details', 'error');
                return;
            }
        }

        this.state.selectedSeats = [];
        this.setBookingStep(2); // Go to Seat Selection
        this.navigate('booking-flow');
        await this.loadSeatMap(flightId);
        this.updateBookingSummary();
    },

    setBookingStep(step) {
        this.state.currentBookingStep = step;

        document.querySelectorAll('.step-item').forEach(el => {
            const s = parseInt(el.getAttribute('data-step'), 10);
            el.classList.remove('active', 'completed');
            if (s === step) el.classList.add('active');
            else if (s < step) el.classList.add('completed');
        });

        document.getElementById('step-panel-seats').style.display = (step === 2) ? 'block' : 'none';
        document.getElementById('step-panel-passengers').style.display = (step === 3) ? 'block' : 'none';
        document.getElementById('step-panel-payment').style.display = (step === 4) ? 'block' : 'none';
        document.getElementById('step-panel-confirmation').style.display = (step === 5) ? 'block' : 'none';
    },

    async loadSeatMap(flightId) {
        const cabinContainer = document.getElementById('cabin-seats-grid');
        cabinContainer.innerHTML = '<div style="text-align:center; padding:20px;">Loading live seat layout...</div>';

        try {
            const res = await API.getFlightSeats(flightId);
            const seats = res.seats || [];

            cabinContainer.innerHTML = '';

            const rowMap = new Map();
            seats.forEach(s => {
                const rowNum = parseInt(s.seat_number.replace(/\D/g, ''), 10);
                if (!rowMap.has(rowNum)) rowMap.set(rowNum, []);
                rowMap.get(rowNum).push(s);
            });

            const bizHeader = document.createElement('div');
            bizHeader.className = 'cabin-section-title';
            bizHeader.textContent = '★ Business Class (Rows 1-2)';
            cabinContainer.appendChild(bizHeader);

            for (let r = 1; r <= 2; r++) {
                if (rowMap.has(r)) {
                    cabinContainer.appendChild(this.createSeatRowElement(r, rowMap.get(r), true));
                }
            }

            const ecoHeader = document.createElement('div');
            ecoHeader.className = 'cabin-section-title';
            ecoHeader.textContent = '✈ Economy Class (Rows 3-10)';
            cabinContainer.appendChild(ecoHeader);

            for (let r = 3; r <= 10; r++) {
                if (rowMap.has(r)) {
                    cabinContainer.appendChild(this.createSeatRowElement(r, rowMap.get(r), false));
                }
            }

        } catch (err) {
            cabinContainer.innerHTML = `<div style="color:var(--danger); text-align:center;">Failed to load seat map: ${err.message}</div>`;
        }
    },

    createSeatRowElement(rowNum, rowSeats, isBusiness) {
        const rowDiv = document.createElement('div');
        rowDiv.className = 'seat-row';

        const rowLabel = document.createElement('div');
        rowLabel.className = 'seat-row-number';
        rowLabel.textContent = rowNum;
        rowDiv.appendChild(rowLabel);

        rowSeats.sort((a, b) => a.seat_number.localeCompare(b.seat_number));

        const splitIndex = isBusiness ? 2 : 3;

        rowSeats.forEach((s, idx) => {
            if (idx === splitIndex) {
                const aisle = document.createElement('div');
                aisle.className = 'cabin-aisle';
                rowDiv.appendChild(aisle);
            }

            const seatBox = document.createElement('div');
            seatBox.className = `seat-box ${s.is_booked ? 'seat-booked' : (isBusiness ? 'seat-business' : 'seat-available')}`;
            seatBox.textContent = s.seat_number;
            seatBox.title = `${s.seat_number} - ${s.seat_class} (Multiplier: ${s.price_multiplier}x)`;

            if (!s.is_booked) {
                seatBox.addEventListener('click', () => this.toggleSeatSelection(s, seatBox));
            }

            rowDiv.appendChild(seatBox);
        });

        return rowDiv;
    },

    toggleSeatSelection(seat, element) {
        const existsIdx = this.state.selectedSeats.findIndex(s => s.seat_id === seat.seat_id);

        if (existsIdx > -1) {
            this.state.selectedSeats.splice(existsIdx, 1);
            element.classList.remove('seat-selected');
        } else {
            if (this.state.selectedSeats.length >= this.state.passengerCount) {
                this.showToast(`You have selected the maximum of ${this.state.passengerCount} seat(s). Deselect a seat to change.`, 'warning');
                return;
            }
            this.state.selectedSeats.push(seat);
            element.classList.add('seat-selected');
        }

        this.updateBookingSummary();
    },

    updateBookingSummary() {
        const f = this.state.selectedFlight;
        if (!f) return;

        document.getElementById('summary-flight-number').textContent = `${f.airline_name} (${f.flight_number})`;
        document.getElementById('summary-route').textContent = `${f.source_code} → ${f.dest_code}`;
        document.getElementById('summary-date').textContent = f.departure_time;

        const seatListStr = this.state.selectedSeats.map(s => s.seat_number).join(', ') || 'None selected';
        document.getElementById('summary-seats-list').textContent = seatListStr;

        let total = 0;
        this.state.selectedSeats.forEach(s => {
            total += f.fare * (s.price_multiplier || 1.0);
        });
        if (this.state.selectedSeats.length === 0) {
            total = f.fare * this.state.passengerCount;
        }

        document.getElementById('summary-base-fare').textContent = `₹${(f.fare * this.state.passengerCount).toLocaleString('en-IN')}`;
        document.getElementById('summary-total-fare').textContent = `₹${Math.round(total).toLocaleString('en-IN')}`;

        const continueBtn = document.getElementById('btn-continue-to-passengers');
        if (continueBtn) {
            continueBtn.disabled = (this.state.selectedSeats.length !== this.state.passengerCount);
            if (continueBtn.disabled) {
                continueBtn.textContent = `Select ${this.state.passengerCount - this.state.selectedSeats.length} more seat(s)`;
            } else {
                continueBtn.textContent = 'Continue to Passenger Details →';
            }
        }
    },

    proceedToPassengers() {
        if (this.state.selectedSeats.length !== this.state.passengerCount) {
            this.showToast(`Please select exactly ${this.state.passengerCount} seat(s) before proceeding.`, 'warning');
            return;
        }

        this.buildPassengerForms();
        this.setBookingStep(3);
    },

    buildPassengerForms() {
        const container = document.getElementById('passengers-form-container');
        container.innerHTML = '';

        const currentUser = API.getCurrentUser() || {};

        this.state.selectedSeats.forEach((seat, idx) => {
            const card = document.createElement('div');
            card.className = 'pax-card';

            const isFirst = (idx === 0);
            const defaultFname = isFirst ? (currentUser.name ? currentUser.name.split(' ')[0] : '') : '';
            const defaultLname = isFirst ? (currentUser.name ? currentUser.name.split(' ').slice(1).join(' ') : '') : '';

            card.innerHTML = `
                <div class="pax-header">
                    <span class="pax-title">Passenger #${idx + 1}</span>
                    <span class="pax-seat-tag">Seat: ${seat.seat_number} (${seat.seat_class})</span>
                </div>
                <div class="pax-grid">
                    <div class="form-group">
                        <label class="form-label">First Name *</label>
                        <input type="text" class="form-control pax-fname" value="${defaultFname}" placeholder="First Name" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Last Name *</label>
                        <input type="text" class="form-control pax-lname" value="${defaultLname}" placeholder="Last Name" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Gender *</label>
                        <select class="form-control pax-gender">
                            <option value="Male">Male</option>
                            <option value="Female">Female</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Age *</label>
                        <input type="number" class="form-control pax-age" min="1" max="110" value="28" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Passport / ID Number</label>
                        <input type="text" class="form-control pax-id" placeholder="Govt ID / Passport (Optional)">
                    </div>
                </div>
            `;
            container.appendChild(card);
        });
    },

    proceedToPayment() {
        const paxCards = document.querySelectorAll('.pax-card');
        const passengers = [];

        for (let i = 0; i < paxCards.length; i++) {
            const card = paxCards[i];
            const fname = card.querySelector('.pax-fname').value.trim();
            const lname = card.querySelector('.pax-lname').value.trim();
            const gender = card.querySelector('.pax-gender').value;
            const age = parseInt(card.querySelector('.pax-age').value, 10);
            const idNum = card.querySelector('.pax-id').value.trim();
            const seat = this.state.selectedSeats[i];

            if (!fname || !lname) {
                this.showToast(`Please enter first and last name for Passenger #${i + 1}.`, 'warning');
                return;
            }
            if (!age || age < 1 || age > 120) {
                this.showToast(`Please enter a valid age for Passenger #${i + 1}.`, 'warning');
                return;
            }

            passengers.push({
                first_name: fname,
                last_name: lname,
                gender: gender,
                age: age,
                id_number: idNum,
                seat_number: seat.seat_number
            });
        }

        this.state.passengersData = passengers;

        this.renderPaymentScreen();
        this.setBookingStep(4);
    },

    renderPaymentScreen() {
        const f = this.state.selectedFlight;
        let total = 0;
        this.state.selectedSeats.forEach(s => {
            total += f.fare * (s.price_multiplier || 1.0);
        });

        document.getElementById('pay-flight-info').textContent = `${f.flight_number}: ${f.source_code} → ${f.dest_code}`;
        document.getElementById('pay-pax-count').textContent = `${this.state.passengersData.length} Passenger(s)`;
        document.getElementById('pay-seats-list').textContent = this.state.selectedSeats.map(s => s.seat_number).join(', ');
        document.getElementById('pay-amount-total').textContent = `₹${Math.round(total).toLocaleString('en-IN')}`;
    },

    async handlePaymentSubmit(e) {
        e.preventDefault();
        const payBtn = document.getElementById('btn-confirm-payment');
        payBtn.disabled = true;
        payBtn.textContent = 'Processing Payment & Booking...';

        try {
            const flightId = this.state.selectedFlight.flight_id;
            const passengers = this.state.passengersData;
            const paymentDetails = {
                payment_method: 'Credit Card (Simulated)',
                card_last4: '7742'
            };

            const res = await API.createBooking(flightId, passengers, paymentDetails);
            this.state.lastBooking = res.booking;

            this.showToast('Booking Confirmed! PNR: ' + res.booking.pnr, 'success');
            this.renderConfirmation(res.booking);
            this.setBookingStep(5);

        } catch (err) {
            this.showToast('Booking failed: ' + err.message, 'error');
            if (err.status === 409) {
                setTimeout(() => {
                    this.setBookingStep(2);
                    this.loadSeatMap(this.state.selectedFlight.flight_id);
                }, 1500);
            }
        } finally {
            payBtn.disabled = false;
            payBtn.textContent = 'Pay & Confirm Reservation';
        }
    },

    renderConfirmation(booking) {
        document.getElementById('confirm-pnr').textContent = booking.pnr;
        document.getElementById('confirm-airline').textContent = `${booking.airline_name} (${booking.flight_number})`;
        document.getElementById('confirm-route').textContent = `${booking.source_city} (${booking.source_code}) → ${booking.dest_city} (${booking.dest_code})`;
        document.getElementById('confirm-dep-time').textContent = booking.departure_time;
        document.getElementById('confirm-arr-time').textContent = booking.arrival_time;
        document.getElementById('confirm-total-fare').textContent = `₹${booking.total_fare.toLocaleString('en-IN')}`;
        document.getElementById('confirm-tx-ref').textContent = booking.transaction_ref || 'TXN-CONFIRMED';

        const paxList = document.getElementById('confirm-pax-list');
        paxList.innerHTML = '';
        (booking.passengers || []).forEach(p => {
            const li = document.createElement('li');
            li.style.padding = '6px 0';
            li.innerHTML = `<strong>${p.first_name} ${p.last_name}</strong> &mdash; Seat: <span class="pax-seat-tag">${p.seat_number}</span> (${p.gender}, Age: ${p.age})`;
            paxList.appendChild(li);
        });
    },

    printBoardingPass() {
        window.print();
    },

    async loadMyBookings() {
        if (!API.isAuthenticated()) {
            this.showToast('Please log in to view your bookings.', 'info');
            this.openModal('modal-passenger-login');
            return;
        }

        const listContainer = document.getElementById('my-bookings-list');
        listContainer.innerHTML = '<div style="text-align:center; padding:30px;">Loading your booking history...</div>';

        try {
            const res = await API.getMyBookings();
            const bookings = res.bookings || [];

            if (bookings.length === 0) {
                listContainer.innerHTML = `
                    <div style="text-align:center; padding:50px 20px; background:white; border-radius:12px; border:1px solid #e2e8f0;">
                        <div style="font-size:2.5rem; margin-bottom:10px;">🎫</div>
                        <h3 style="color:var(--primary-navy);">No Bookings Found</h3>
                        <p style="color:var(--text-muted); margin-bottom:20px;">You haven't made any flight reservations yet.</p>
                        <button class="btn btn-primary" onclick="App.navigate('home')">Search Flights Now</button>
                    </div>
                `;
                return;
            }

            listContainer.innerHTML = '';
            bookings.forEach(b => {
                const card = document.createElement('div');
                card.className = 'flight-card';
                card.style.gridTemplateColumns = '1.8fr 2.5fr 1.5fr auto';

                const statusClass = (b.booking_status === 'CONFIRMED') ? 'status-confirmed' : 'status-cancelled';

                card.innerHTML = `
                    <div>
                        <div style="font-size:0.8rem; color:var(--text-muted);">PNR NUMBER</div>
                        <div style="font-size:1.4rem; font-weight:800; font-family:monospace; color:var(--sky-blue);">${b.pnr}</div>
                        <div style="font-size:0.85rem; color:var(--text-muted);">${b.booking_date}</div>
                    </div>

                    <div>
                        <div style="font-weight:700; font-size:1.1rem; color:var(--primary-navy);">${b.source_city} (${b.source_code}) &rarr; ${b.dest_city} (${b.dest_code})</div>
                        <div style="font-size:0.85rem; color:var(--text-muted);">${b.airline_name} &bull; ${b.flight_number}</div>
                        <div style="font-size:0.85rem; margin-top:4px;">
                            Seats: <strong>${b.seats_list ? b.seats_list.join(', ') : 'Assigned'}</strong>
                        </div>
                    </div>

                    <div style="text-align:right;">
                        <span class="status-badge ${statusClass}">${b.booking_status}</span>
                        <div style="font-size:1.3rem; font-weight:800; margin-top:8px;">₹${b.total_fare.toLocaleString('en-IN')}</div>
                    </div>

                    <div style="display:flex; flex-direction:column; gap:8px;">
                        <button class="btn btn-outline btn-sm" onclick="App.viewBookingDetails('${b.pnr}')">
                            View Details
                        </button>
                        ${b.booking_status === 'CONFIRMED' ? `
                            <button class="btn btn-danger btn-sm" onclick="App.promptCancelBooking('${b.pnr}', ${b.total_fare})">
                                Cancel Ticket
                            </button>
                        ` : ''}
                    </div>
                `;
                listContainer.appendChild(card);
            });

        } catch (err) {
            listContainer.innerHTML = `<div style="color:var(--danger); text-align:center;">Failed to load bookings: ${err.message}</div>`;
        }
    },

    async viewBookingDetails(pnr) {
        try {
            const res = await API.getBookingByPNR(pnr);
            const b = res.booking;
            this.renderConfirmation(b);
            this.setBookingStep(5);
            this.navigate('booking-flow');
        } catch (err) {
            this.showToast('Could not find booking: ' + err.message, 'error');
        }
    },

    promptCancelBooking(pnr, totalFare) {
        const refund = Math.round(totalFare * 0.9);
        const confirmed = confirm(
            `Are you sure you want to cancel reservation ${pnr}?\n\n` +
            `Cancellation Policy:\n` +
            `• Original Fare: ₹${totalFare.toLocaleString('en-IN')}\n` +
            `• Refund Amount (90%): ₹${refund.toLocaleString('en-IN')}\n` +
            `• Cancellation Fee (10%): ₹${(totalFare - refund).toLocaleString('en-IN')}\n\n` +
            `Your seats will be released immediately.`
        );

        if (confirmed) {
            this.executeCancelBooking(pnr);
        }
    },

    async executeCancelBooking(pnr) {
        try {
            const res = await API.cancelBooking(pnr);
            this.showToast(res.message, 'success');
            this.loadMyBookings();
        } catch (err) {
            this.showToast('Cancellation failed: ' + err.message, 'error');
        }
    },

    async loadAdminDashboard() {
        try {
            const res = await API.getAdminStats();
            const s = res.stats || {};

            document.getElementById('stat-total-flights').textContent = s.total_flights || 0;
            document.getElementById('stat-total-bookings').textContent = s.total_bookings || 0;
            document.getElementById('stat-confirmed-bookings').textContent = s.confirmed_bookings || 0;
            document.getElementById('stat-cancelled-bookings').textContent = s.cancelled_bookings || 0;
            document.getElementById('stat-total-revenue').textContent = `₹${(s.total_revenue || 0).toLocaleString('en-IN')}`;
            document.getElementById('stat-occupancy-rate').textContent = `${s.occupancy_rate || 0}%`;
            document.getElementById('stat-total-users').textContent = s.total_users || 0;

            if (this.state.activeAdminTab === 'flights') {
                this.loadAdminFlights();
            } else if (this.state.activeAdminTab === 'bookings') {
                this.loadAdminBookings();
            }

        } catch (err) {
            this.showToast('Failed to load admin stats: ' + err.message, 'error');
        }
    },

    async loadAdminFlights() {
        const tableBody = document.getElementById('admin-flights-table-body');
        tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Loading flights...</td></tr>';

        const search = document.getElementById('admin-flight-search')?.value || '';
        const status = document.getElementById('admin-flight-filter-status')?.value || 'ALL';

        try {
            const res = await API.getAdminFlights(search, status);
            const flights = res.flights || [];

            if (flights.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">No flights match criteria.</td></tr>';
                return;
            }

            tableBody.innerHTML = '';
            flights.forEach(f => {
                const tr = document.createElement('tr');
                const statusClass = (f.status === 'SCHEDULED' || f.status === 'ACTIVE') ? 'status-scheduled' : 'status-cancelled';

                tr.innerHTML = `
                    <td><strong>${f.flight_number}</strong><br><small style="color:var(--text-muted);">${f.aircraft_model}</small></td>
                    <td>${f.airline_name}</td>
                    <td>${f.source_code} &rarr; ${f.dest_code}</td>
                    <td>${f.departure_time}</td>
                    <td>₹${f.fare.toLocaleString('en-IN')}</td>
                    <td>${f.booked_seats} / ${f.total_seats} (${f.occupancy_pct}%)</td>
                    <td><span class="status-badge ${statusClass}">${f.status}</span></td>
                    <td>
                        <button class="btn btn-outline btn-sm" onclick="App.openEditFlightModal(${f.flight_id}, ${f.fare}, '${f.status}')">Edit</button>
                        ${f.status !== 'CANCELLED' ? `
                            <button class="btn btn-danger btn-sm" onclick="App.cancelFlightAdmin(${f.flight_id}, '${f.flight_number}')">Cancel</button>
                        ` : ''}
                    </td>
                `;
                tableBody.appendChild(tr);
            });

        } catch (err) {
            tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger); text-align:center;">${err.message}</td></tr>`;
        }
    },

    openAddFlightModal() {
        this.openModal('modal-add-flight');
    },

    async handleAddFlightSubmit(e) {
        e.preventDefault();
        const data = {
            flight_number: document.getElementById('admin-flight-number').value,
            airline_name: document.getElementById('admin-flight-airline').value,
            source_airport_id: document.getElementById('admin-flight-source').value,
            destination_airport_id: document.getElementById('admin-flight-dest').value,
            departure_time: document.getElementById('admin-flight-dep-time').value,
            arrival_time: document.getElementById('admin-flight-arr-time').value,
            fare: parseFloat(document.getElementById('admin-flight-fare').value),
            total_seats: parseInt(document.getElementById('admin-flight-seats').value, 10) || 56,
            aircraft_model: document.getElementById('admin-flight-model').value
        };

        try {
            await API.addFlight(data);
            this.showToast(`Flight ${data.flight_number} created successfully with 56 seats generated!`, 'success');
            this.closeModals();
            this.loadAdminFlights();
            this.loadAdminDashboard();
        } catch (err) {
            this.showToast('Failed to add flight: ' + err.message, 'error');
        }
    },

    openEditFlightModal(flightId, currentFare, currentStatus) {
        document.getElementById('edit-flight-id').value = flightId;
        document.getElementById('edit-flight-fare').value = currentFare;
        document.getElementById('edit-flight-status').value = currentStatus;
        this.openModal('modal-edit-flight');
    },

    async handleEditFlightSubmit(e) {
        e.preventDefault();
        const flightId = parseInt(document.getElementById('edit-flight-id').value, 10);
        const data = {
            fare: parseFloat(document.getElementById('edit-flight-fare').value),
            status: document.getElementById('edit-flight-status').value
        };

        try {
            await API.updateFlight(flightId, data);
            this.showToast(`Flight #${flightId} updated successfully.`, 'success');
            this.closeModals();
            this.loadAdminFlights();
        } catch (err) {
            this.showToast('Failed to update flight: ' + err.message, 'error');
        }
    },

    async cancelFlightAdmin(flightId, flightNum) {
        if (!confirm(`Are you sure you want to cancel flight ${flightNum}? This will mark the flight as CANCELLED.`)) {
            return;
        }

        try {
            await API.cancelFlightAdmin(flightId);
            this.showToast(`Flight ${flightNum} marked as CANCELLED.`, 'success');
            this.loadAdminFlights();
            this.loadAdminDashboard();
        } catch (err) {
            this.showToast('Failed to cancel flight: ' + err.message, 'error');
        }
    },

    async loadAdminBookings() {
        const tableBody = document.getElementById('admin-bookings-table-body');
        tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Loading reservations...</td></tr>';

        const search = document.getElementById('admin-booking-search')?.value || '';
        const status = document.getElementById('admin-booking-filter-status')?.value || 'ALL';

        try {
            const res = await API.getAdminBookings(search, status);
            const bookings = res.bookings || [];

            if (bookings.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">No bookings found.</td></tr>';
                return;
            }

            tableBody.innerHTML = '';
            bookings.forEach(b => {
                const tr = document.createElement('tr');
                const statusClass = (b.booking_status === 'CONFIRMED') ? 'status-confirmed' : 'status-cancelled';

                tr.innerHTML = `
                    <td><strong style="color:var(--sky-blue); font-family:monospace;">${b.pnr}</strong><br><small>${b.booking_date}</small></td>
                    <td><strong>${b.customer_name}</strong><br><small style="color:var(--text-muted);">${b.customer_email}</small></td>
                    <td>${b.flight_number} (${b.source_code} &rarr; ${b.dest_code})</td>
                    <td>${b.seats || 'N/A'}</td>
                    <td>₹${b.total_fare.toLocaleString('en-IN')}</td>
                    <td><span class="status-badge ${statusClass}">${b.booking_status}</span></td>
                    <td>
                        <button class="btn btn-outline btn-sm" onclick="App.viewBookingDetails('${b.pnr}')">Details</button>
                        ${b.booking_status === 'CONFIRMED' ? `
                            <button class="btn btn-danger btn-sm" onclick="App.promptCancelBooking('${b.pnr}', ${b.total_fare})">Refund</button>
                        ` : ''}
                    </td>
                `;
                tableBody.appendChild(tr);
            });

        } catch (err) {
            tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger); text-align:center;">${err.message}</td></tr>`;
        }
    },

    async loadSeatTrackerOptions() {
        const select = document.getElementById('tracker-flight-select');
        select.innerHTML = '<option value="">Select a flight to track seats...</option>';

        try {
            const res = await API.getAdminFlights('', 'ALL');
            (res.flights || []).forEach(f => {
                const opt = document.createElement('option');
                opt.value = f.flight_id;
                opt.textContent = `${f.flight_number} - ${f.source_code} to ${f.dest_code} (${f.departure_time})`;
                select.appendChild(opt);
            });
        } catch (e) {
            console.error(e);
        }
    },

    async handleTrackerFlightChange() {
        const flightId = document.getElementById('tracker-flight-select').value;
        const container = document.getElementById('tracker-cabin-display');

        if (!flightId) {
            container.innerHTML = '<div style="text-align:center; padding:30px; color:var(--text-muted);">Please select a flight from the dropdown above.</div>';
            return;
        }

        container.innerHTML = '<div style="text-align:center; padding:30px;">Loading cabin occupancy tracker...</div>';

        try {
            const data = await API.getFlightTracker(flightId);
            const f = data.flight;
            const seats = data.seats || [];

            container.innerHTML = `
                <div style="background:#ffffff; border-radius:12px; padding:20px; border:1px solid var(--border-color); margin-bottom:20px;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <div>
                            <h3 style="color:var(--primary-navy);">${f.airline_name} (${f.flight_number})</h3>
                            <div style="color:var(--text-muted);">${f.source_city} &rarr; ${f.dest_city} &bull; ${f.departure_time}</div>
                        </div>
                        <div style="display:flex; gap:20px; text-align:right;">
                            <div>
                                <div style="font-size:0.8rem; color:var(--text-muted);">TOTAL SEATS</div>
                                <div style="font-size:1.3rem; font-weight:700;">${data.total_seats}</div>
                            </div>
                            <div>
                                <div style="font-size:0.8rem; color:var(--text-muted);">BOOKED</div>
                                <div style="font-size:1.3rem; font-weight:700; color:var(--danger);">${data.booked_seats}</div>
                            </div>
                            <div>
                                <div style="font-size:0.8rem; color:var(--text-muted);">AVAILABLE</div>
                                <div style="font-size:1.3rem; font-weight:700; color:var(--success);">${data.available_seats}</div>
                            </div>
                            <div>
                                <div style="font-size:0.8rem; color:var(--text-muted);">OCCUPANCY</div>
                                <div style="font-size:1.3rem; font-weight:800; color:var(--sky-blue);">${data.occupancy_pct}%</div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="airplane-cabin" style="max-width:550px;">
                    <div class="seat-legend">
                        <div class="legend-item"><div class="seat-box seat-available"></div> Available</div>
                        <div class="legend-item"><div class="seat-box seat-booked"></div> Occupied / Booked</div>
                        <div class="legend-item"><div class="seat-box seat-business"></div> Business</div>
                    </div>
                    <div id="tracker-seat-rows"></div>
                </div>
            `;

            const rowsContainer = container.querySelector('#tracker-seat-rows');
            const rowMap = new Map();
            seats.forEach(s => {
                const rNum = parseInt(s.seat_number.replace(/\D/g, ''), 10);
                if (!rowMap.has(rNum)) rowMap.set(rNum, []);
                rowMap.get(rNum).push(s);
            });

            for (let r = 1; r <= 10; r++) {
                if (rowMap.has(r)) {
                    rowsContainer.appendChild(this.createSeatRowElement(r, rowMap.get(r), r <= 2));
                }
            }

        } catch (err) {
            container.innerHTML = `<div style="color:var(--danger); text-align:center;">Failed to load tracker: ${err.message}</div>`;
        }
    },

    handleSchedulingAlgoChange() {
        const algo = document.getElementById('sched-algo-select').value;
        const qWrap = document.getElementById('sched-quantum-wrap');
        if (qWrap) {
            qWrap.style.display = (algo === 'RR') ? 'block' : 'none';
        }
        this.loadSchedulingSimulation();
    },

    async loadSchedulingSimulation() {
        const algo = document.getElementById('sched-algo-select')?.value || 'FCFS';
        const quantum = parseInt(document.getElementById('sched-quantum-select')?.value || '2', 10);

        try {
            const data = await API.getCPUScheduling(algo, quantum);

            document.getElementById('sched-type-val').textContent = data.type || 'Non-Preemptive';
            document.getElementById('sched-awt-val').textContent = `${data.avg_waiting_time} ms`;
            document.getElementById('sched-atat-val').textContent = `${data.avg_turnaround_time} ms`;
            document.getElementById('sched-total-time-val').textContent = `${data.total_completion_time} ms`;

            const ganttBox = document.getElementById('sched-gantt-container');
            if (ganttBox && data.gantt_chart) {
                ganttBox.innerHTML = '';
                const colors = ['#0284c7', '#0d9488', '#e11d48', '#8b5cf6', '#d97706', '#64748b'];

                data.gantt_chart.forEach((slot, i) => {
                    const block = document.createElement('div');
                    const color = slot.pid === 'IDLE' ? '#94a3b8' : colors[i % colors.length];
                    const widthPx = Math.max(slot.duration * 45, 60);

                    block.style.cssText = `
                        background: ${color};
                        color: white;
                        min-width: ${widthPx}px;
                        padding: 10px 8px;
                        text-align: center;
                        border-radius: 6px;
                        font-size: 0.8rem;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                        display: flex;
                        flex-direction: column;
                        justify-content: space-between;
                    `;

                    block.innerHTML = `
                        <div style="font-weight:800; font-size:0.95rem;">${slot.pid}</div>
                        <div style="font-size:0.7rem; opacity:0.9; text-overflow:ellipsis; overflow:hidden; white-space:nowrap;" title="${slot.taskName}">${slot.taskName}</div>
                        <div style="margin-top:6px; font-size:0.75rem; border-top:1px solid rgba(255,255,255,0.4); padding-top:2px;">
                            ${slot.startTime} &rarr; ${slot.endTime}
                        </div>
                    `;
                    ganttBox.appendChild(block);
                });
            }

            const tbody = document.getElementById('sched-process-table-body');
            if (tbody && data.processes) {
                tbody.innerHTML = '';
                data.processes.forEach(p => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td><strong style="color:var(--sky-blue); font-family:monospace; font-size:1rem;">${p.pid}</strong></td>
                        <td><strong>${p.task_name}</strong></td>
                        <td>${p.arrival_time} ms</td>
                        <td>${p.burst_time} ms</td>
                        <td><span style="font-weight:700;">${p.completion_time} ms</span></td>
                        <td><span style="color:var(--success); font-weight:700;">${p.turnaround_time} ms</span></td>
                        <td><span style="color:var(--sky-blue); font-weight:700;">${p.waiting_time} ms</span></td>
                    `;
                    tbody.appendChild(tr);
                });
            }

        } catch (err) {
            this.showToast('Failed to run CPU scheduling: ' + err.message, 'error');
        }
    }
};

document.addEventListener('DOMContentLoaded', () => App.init());
