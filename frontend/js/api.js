
const API = {
    BASE_URL: '',

    getToken() {
        return localStorage.getItem('ars_token');
    },

    setToken(token) {
        if (token) {
            localStorage.setItem('ars_token', token);
        } else {
            localStorage.removeItem('ars_token');
        }
    },

    getCurrentUser() {
        const u = localStorage.getItem('ars_user');
        return u ? JSON.parse(u) : null;
    },

    setCurrentUser(user) {
        if (user) {
            localStorage.setItem('ars_user', JSON.stringify(user));
        } else {
            localStorage.removeItem('ars_user');
        }
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    isAdmin() {
        const user = this.getCurrentUser();
        return user && user.role === 'admin';
    },

    async request(endpoint, options = {}) {
        const url = `${this.BASE_URL}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);
            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                const error = new Error(data.error || `HTTP ${response.status}: Request failed`);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (err) {
            console.error(`API Error [${endpoint}]:`, err);
            throw err;
        }
    },

    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    async register(name, email, password, phone) {
        const res = await this.post('/api/auth/register', { name, email, password, phone });
        if (res.user && res.user.token) {
            this.setToken(res.user.token);
            this.setCurrentUser(res.user);
        }
        return res;
    },

    async login(email, password, requiredRole = null) {
        const res = await this.post('/api/auth/login', { email, password, required_role: requiredRole });
        if (res.user && res.user.token) {
            this.setToken(res.user.token);
            this.setCurrentUser(res.user);
        }
        return res;
    },

    async logout() {
        try {
            await this.post('/api/auth/logout', {});
        } catch (e) {
        }
        this.setToken(null);
        this.setCurrentUser(null);
    },

    async getMe() {
        return this.get('/api/auth/me');
    },

    async getAirports() {
        return this.get('/api/airports');
    },

    async searchFlights(source, dest, date, passengers) {
        const params = new URLSearchParams();
        if (source) params.append('source', source);
        if (dest) params.append('dest', dest);
        if (date) params.append('date', date);
        if (passengers) params.append('passengers', passengers);
        return this.get(`/api/flights/search?${params.toString()}`);
    },

    async getFlightDetails(flightId) {
        return this.get(`/api/flights/${flightId}`);
    },

    async getFlightSeats(flightId) {
        return this.get(`/api/flights/${flightId}/seats`);
    },

    async createBooking(flightId, passengers, paymentDetails) {
        return this.post('/api/bookings/create', {
            flight_id: flightId,
            passengers,
            payment_details: paymentDetails
        });
    },

    async getMyBookings() {
        return this.get('/api/bookings/my');
    },

    async getBookingByPNR(pnr) {
        return this.get(`/api/bookings/${pnr}`);
    },

    async cancelBooking(pnr) {
        return this.post('/api/bookings/cancel', { pnr });
    },

    async getAdminStats() {
        return this.get('/api/admin/stats');
    },

    async getAdminFlights(search = '', status = 'ALL') {
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        if (status) params.append('status', status);
        return this.get(`/api/admin/flights?${params.toString()}`);
    },

    async addFlight(flightData) {
        return this.post('/api/admin/flights/add', flightData);
    },

    async updateFlight(flightId, flightData) {
        return this.post('/api/admin/flights/update', { flight_id: flightId, ...flightData });
    },

    async cancelFlightAdmin(flightId) {
        return this.post('/api/admin/flights/cancel', { flight_id: flightId });
    },

    async getAdminBookings(search = '', status = 'ALL') {
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        if (status) params.append('status', status);
        return this.get(`/api/admin/bookings?${params.toString()}`);
    },

    async getFlightTracker(flightId) {
        return this.get(`/api/admin/tracker/${flightId}`);
    },

    async getCPUScheduling(algo = 'FCFS', quantum = 2) {
        return this.get(`/api/admin/scheduling?algo=${algo}&quantum=${quantum}`);
    }
};
