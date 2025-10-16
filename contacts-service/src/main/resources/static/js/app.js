const state = {
    user: null,
    contacts: [],
    editingContactId: null
};

const elements = {
    contactsContainer: document.getElementById('contactsContainer'),
    loadingState: document.getElementById('loadingState'),
    addContactBtn: document.getElementById('addContactBtn'),
    exportBtn: document.getElementById('exportBtn'),
    loginLink: document.getElementById('loginLink'),
    signupLink: document.getElementById('signupLink'),
    logoutForm: document.getElementById('logoutForm'),
    searchInput: document.getElementById('searchInput'),
    contactModal: new bootstrap.Modal(document.getElementById('contactModal')),
    contactModalTitle: document.getElementById('contactModalTitle'),
    contactForm: document.getElementById('contactForm'),
    modalAlert: document.getElementById('modalAlert'),
    nameInput: document.getElementById('contactName'),
    addressInput: document.getElementById('contactAddress'),
    pictureInput: document.getElementById('contactPicture')
};

const debounce = (fn, delay = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
};

async function fetchCurrentUser() {
    try {
        const response = await fetch('/api/auth/me', {credentials: 'include'});
        if (response.ok) {
            state.user = await response.json();
        } else {
            state.user = null;
        }
    } catch (error) {
        console.error('Failed to determine user', error);
        state.user = null;
    }
    updateAuthUi();
}

function updateAuthUi() {
    const isAuthenticated = Boolean(state.user);
    elements.addContactBtn.classList.toggle('d-none', !isAuthenticated);
    elements.exportBtn.classList.toggle('d-none', !isAuthenticated);
    elements.logoutForm.classList.toggle('d-none', !isAuthenticated);
    elements.loginLink.classList.toggle('d-none', isAuthenticated);
    elements.signupLink.classList.toggle('d-none', isAuthenticated);
}

async function loadContacts(search = '') {
    if (elements.loadingState) {
        elements.loadingState.classList.remove('d-none');
    }
    try {
        const params = new URLSearchParams();
        if (search) {
            params.append('search', search);
        }
        const response = await fetch(`/api/contacts?${params.toString()}`, {credentials: 'include'});
        state.contacts = await response.json();
        renderContacts();
    } catch (error) {
        console.error('Unable to load contacts', error);
        elements.contactsContainer.innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">Unable to load contacts right now.</div>
            </div>`;
    } finally {
        if (elements.loadingState) {
            elements.loadingState.classList.add('d-none');
        }
    }
}

function renderContacts() {
    if (!state.contacts.length) {
        elements.contactsContainer.innerHTML = `
            <div class="col-12 text-center py-5 text-muted">
                No contacts yet. ${state.user ? 'Add one using the button above.' : 'Sign in to create and manage contacts.'}
            </div>`;
        return;
    }

    elements.contactsContainer.innerHTML = state.contacts.map(contact => {
        const isOwner = state.user && state.user.username === contact.ownerUsername;
        const weather = contact.weather
            ? `<span class="badge rounded-pill text-bg-info weather-pill">${contact.weather.description} · ${contact.weather.temperatureCelsius.toFixed(1)}°C</span>`
            : '';
        const buttons = isOwner ? `
            <div class="d-flex gap-2 mt-2">
                <button class="btn btn-sm btn-outline-primary" data-action="edit" data-id="${contact.id}">Edit</button>
                <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${contact.id}">Delete</button>
            </div>` : '';
        const picture = contact.pictureUrl
            ? `<img src="${contact.pictureUrl}" alt="${contact.name}" class="contact-card-img" onerror="this.src='https://via.placeholder.com/400x180?text=No+Image';">`
            : `<img src="https://via.placeholder.com/400x180?text=No+Image" alt="No image" class="contact-card-img">`;

        return `
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card h-100 contact-card">
                    ${picture}
                    <div class="card-body d-flex flex-column">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h2 class="h5 card-title mb-0">${contact.name}</h2>
                            ${weather}
                        </div>
                        <p class="card-text flex-grow-1">${contact.address}</p>
                        <div class="mt-3">
                            <span class="badge text-bg-secondary">Owner: ${contact.ownerUsername}</span>
                        </div>
                        ${buttons}
                    </div>
                </div>
            </div>`;
    }).join('');

    elements.contactsContainer.querySelectorAll('button[data-action="edit"]').forEach(button => {
        button.addEventListener('click', () => openEditModal(button.dataset.id));
    });
    elements.contactsContainer.querySelectorAll('button[data-action="delete"]').forEach(button => {
        button.addEventListener('click', () => deleteContact(button.dataset.id));
    });
}

function openCreateModal() {
    state.editingContactId = null;
    elements.contactModalTitle.textContent = 'New Contact';
    elements.contactForm.reset();
    elements.modalAlert.innerHTML = '';
    elements.contactModal.show();
}

function openEditModal(id) {
    const contact = state.contacts.find(item => String(item.id) === String(id));
    if (!contact) {
        return;
    }
    state.editingContactId = contact.id;
    elements.contactModalTitle.textContent = 'Edit Contact';
    elements.nameInput.value = contact.name;
    elements.addressInput.value = contact.address;
    elements.pictureInput.value = contact.pictureUrl || '';
    elements.modalAlert.innerHTML = '';
    elements.contactModal.show();
}

async function upsertContact(event) {
    event.preventDefault();
    elements.modalAlert.innerHTML = '';
    const payload = {
        name: elements.nameInput.value.trim(),
        address: elements.addressInput.value.trim(),
        pictureUrl: elements.pictureInput.value.trim()
    };

    const method = state.editingContactId ? 'PUT' : 'POST';
    const url = state.editingContactId
        ? `/api/contacts/${state.editingContactId}`
        : '/api/contacts';

    try {
        const response = await fetch(url, {
            method,
            credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.error || Object.values(data).join(', ') || 'Unable to save contact.');
        }
        elements.contactModal.hide();
        await loadContacts(elements.searchInput.value.trim());
    } catch (error) {
        elements.modalAlert.innerHTML = `
            <div class="alert alert-danger" role="alert">${error.message}</div>
        `;
    }
}

async function deleteContact(id) {
    if (!confirm('Delete this contact?')) {
        return;
    }
    try {
        const response = await fetch(`/api/contacts/${id}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        if (response.status === 204) {
            await loadContacts(elements.searchInput.value.trim());
        } else {
            throw new Error('Unable to delete contact.');
        }
    } catch (error) {
        alert(error.message);
    }
}

function exportCsv() {
    window.location.href = '/api/contacts/export';
}

function registerEventListeners() {
    elements.addContactBtn?.addEventListener('click', openCreateModal);
    elements.contactForm?.addEventListener('submit', upsertContact);
    elements.searchInput?.addEventListener('input', debounce(event => {
        loadContacts(event.target.value.trim());
    }, 350));
    elements.exportBtn?.addEventListener('click', exportCsv);
}

(async function init() {
    registerEventListeners();
    await fetchCurrentUser();
    await loadContacts();
})();
