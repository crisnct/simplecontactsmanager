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
    pictureInput: document.getElementById('contactPicture'),
    currentPictureWrapper: document.getElementById('currentPictureWrapper'),
    currentPicturePreview: document.getElementById('currentPicturePreview')
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
        const response = await fetch('/api/auth/me', { credentials: 'include' });
        if (response.ok) {
            const data = await response.json();
            state.user = data.authenticated ? { username: data.username } : null;
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
        const response = await fetch(`/api/contacts?${params.toString()}`, { credentials: 'include' });
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
            ? `<span class="badge rounded-pill text-bg-info weather-pill">${contact.weather.description} &bull; ${contact.weather.temperatureCelsius.toFixed(1)}°C</span>`
            : '';
        const pictureMarkup = contact.hasPicture
            ? `<img src="/api/contacts/${contact.id}/picture?ts=${encodeURIComponent(contact.updatedAt)}" alt="${contact.name}" class="contact-card-img">`
            : `<div class="contact-card-img-placeholder">No image</div>`;
        const buttons = isOwner ? `
            <div class="d-flex gap-2 mt-3">
                <button class="btn btn-sm btn-outline-primary" data-action="edit" data-id="${contact.id}">Edit</button>
                <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${contact.id}">Delete</button>
            </div>` : '';

        return `
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card h-100 contact-card">
                    <div class="card-body d-flex flex-column">
                        <div class="d-flex gap-3 align-items-start mb-3">
                            <div class="flex-shrink-0 text-center">
                                ${pictureMarkup}
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex justify-content-between align-items-start">
                                    <h2 class="h5 card-title mb-0">${contact.name}</h2>
                                    ${weather}
                                </div>
                                <p class="card-text mt-2 mb-0">${contact.address}</p>
                            </div>
                        </div>
                        <div class="mt-auto">
                            <span class="badge text-bg-secondary">Owner: ${contact.ownerUsername}</span>
                            ${buttons}
                        </div>
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
    if (elements.currentPictureWrapper) {
        elements.currentPictureWrapper.classList.add('d-none');
    }
    if (elements.currentPicturePreview) {
        elements.currentPicturePreview.src = '';
        elements.currentPicturePreview.alt = 'Contact picture placeholder';
    }
    elements.contactModal.show();
}

function openEditModal(id) {
    const contact = state.contacts.find(item => String(item.id) === String(id));
    if (!contact) {
        return;
    }
    state.editingContactId = contact.id;
    elements.contactModalTitle.textContent = 'Edit Contact';
    elements.contactForm.reset();
    elements.nameInput.value = contact.name;
    elements.addressInput.value = contact.address;
    if (elements.pictureInput) {
        elements.pictureInput.value = '';
    }
    if (elements.currentPictureWrapper) {
        if (contact.hasPicture) {
            elements.currentPictureWrapper.classList.remove('d-none');
            if (elements.currentPicturePreview) {
                elements.currentPicturePreview.src = `/api/contacts/${contact.id}/picture?ts=${encodeURIComponent(contact.updatedAt)}`;
                elements.currentPicturePreview.alt = `${contact.name} picture`;
            }
        } else {
            elements.currentPictureWrapper.classList.add('d-none');
            if (elements.currentPicturePreview) {
                elements.currentPicturePreview.src = '';
                elements.currentPicturePreview.alt = 'Contact picture placeholder';
            }
        }
    }
    elements.modalAlert.innerHTML = '';
    elements.contactModal.show();
}

async function upsertContact(event) {
    event.preventDefault();
    elements.modalAlert.innerHTML = '';
    const formData = new FormData();
    formData.append('name', elements.nameInput.value.trim());
    formData.append('address', elements.addressInput.value.trim());
    if (elements.pictureInput?.files && elements.pictureInput.files[0]) {
        formData.append('picture', elements.pictureInput.files[0]);
    }

    const method = state.editingContactId ? 'PUT' : 'POST';
    const url = state.editingContactId
        ? `/api/contacts/${state.editingContactId}`
        : '/api/contacts';

    try {
        const response = await fetch(url, {
            method,
            credentials: 'include',
            body: formData
        });
        if (!response.ok) {
            let message = 'Unable to save contact.';
            const contentType = response.headers.get('Content-Type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                message = data.message || data.error || Object.values(data).join(', ') || message;
            }
            throw new Error(message);
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
