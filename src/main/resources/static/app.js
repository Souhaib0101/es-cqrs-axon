'use strict';

const $ = id => document.getElementById(id);
let selectedAccount = null;
let stream = null;
let detailRequest = 0;

function message(text, error = false) {
    $('message').textContent = text;
    $('message').className = error ? 'error' : '';
}

async function request(path, method = 'GET', body) {
    const response = await fetch(path, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined
    });
    const text = await response.text();
    const data = text && response.headers.get('content-type')?.includes('json') ? JSON.parse(text) : null;
    if (!response.ok) throw new Error(data?.detail || `Request failed (${response.status})`);
    return data;
}

function element(tag, text) {
    const node = document.createElement(tag);
    node.textContent = text;
    return node;
}

function money(amount, currency) {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
}

function renderAccount(account) {
    selectedAccount = account;
    $('balance').textContent = money(account.balance, account.currency);
    $('account-id').textContent = account.id;
    $('account-status').textContent = account.status;
    $('status-button').textContent = account.status === 'ACTIVATED' ? 'Suspend account' : 'Activate account';
    $('status-button').disabled = false;
    $('operation-button').disabled = account.status !== 'ACTIVATED';
}

async function loadDetails(id) {
    const version = ++detailRequest;
    const [statement, events] = await Promise.all([
        request(`/query/accounts/${id}/statement`), request(`/query/accounts/${id}/events`)
    ]);
    if (version !== detailRequest || $('account-select').value !== id) return;
    renderAccount(statement.account);
    const rows = statement.operations.map(operation => {
        const row = document.createElement('tr');
        row.append(element('td', new Date(operation.operationDate).toLocaleString()),
            element('td', operation.type), element('td', money(operation.amount, operation.currency)));
        return row;
    });
    if (!rows.length) {
        const row = document.createElement('tr');
        const cell = element('td', 'No operations yet.');
        cell.colSpan = 3;
        row.append(cell);
        rows.push(row);
    }
    $('operations').replaceChildren(...rows);
    $('event-count').textContent = `${events.length} events`;
    $('events').replaceChildren(...events.map(event => {
        const item = document.createElement('li');
        item.append(element('strong', `#${event.sequenceNumber} ${event.type}`),
            element('small', new Date(event.timestamp).toLocaleString()),
            element('pre', JSON.stringify(event.payload, null, 2)));
        return item;
    }));
}

async function selectAccount(id) {
    stream?.close();
    stream = null;
    selectedAccount = null;
    detailRequest++;
    $('status-button').disabled = true;
    $('operation-button').disabled = true;
    $('balance').textContent = '—';
    $('account-id').textContent = id || 'Create an account to get started.';
    $('account-status').textContent = '—';
    $('connection').textContent = id ? 'Connecting…' : 'No account selected';
    $('events').replaceChildren();
    $('event-count').textContent = '0 events';
    const row = document.createElement('tr');
    const cell = element('td', id ? 'Loading operations…' : 'No operations yet.');
    cell.colSpan = 3;
    row.append(cell);
    $('operations').replaceChildren(row);
    if (!id) return;
    await loadDetails(id);
    if ($('account-select').value !== id) return;
    const connection = new EventSource(`/query/accounts/${id}/watch`);
    stream = connection;
    connection.onopen = () => { if (stream === connection) $('connection').textContent = '● Live'; };
    connection.onerror = () => { if (stream === connection) $('connection').textContent = 'Reconnecting…'; };
    connection.onmessage = () => {
        if (stream === connection) loadDetails(id).catch(error => message(error.message, true));
    };
}

async function loadAccounts(preferredId) {
    const previousId = preferredId || $('account-select').value;
    const accounts = await request('/query/accounts');
    $('account-select').replaceChildren(...accounts.map(account => {
        const option = element('option', `${account.currency} · ${account.id.slice(0, 8)} · ${account.status}`);
        option.value = account.id;
        return option;
    }));
    if (!accounts.length) {
        const option = element('option', 'No accounts yet');
        option.value = '';
        $('account-select').append(option);
    } else if (accounts.some(account => account.id === previousId)) {
        $('account-select').value = previousId;
    }
    await selectAccount($('account-select').value);
}

async function submit(form, action) {
    const button = form.querySelector('button');
    button.disabled = true;
    message('Submitting command…');
    try {
        await action(new FormData(form));
        message('Command accepted. The read model will update automatically.');
    } catch (error) {
        message(error.message, true);
    } finally {
        button.disabled = form.id === 'operation-form' && selectedAccount?.status !== 'ACTIVATED';
    }
}

$('create-form').addEventListener('submit', event => {
    event.preventDefault();
    submit(event.currentTarget, async data => {
        const result = await request('/commands/accounts', 'POST', {
            initialBalance: data.get('initialBalance'), currency: data.get('currency')
        });
        // Command success precedes the tracking processor's projection commit.
        for (let attempt = 0; attempt < 50; attempt++) {
            const accounts = await request('/query/accounts');
            if (accounts.some(account => account.id === result.accountId)) {
                await loadAccounts(result.accountId);
                return;
            }
            await new Promise(resolve => setTimeout(resolve, 200));
        }
        throw new Error('Account created; the read model is catching up. Click Refresh shortly.');
    });
});

$('operation-form').addEventListener('submit', event => {
    event.preventDefault();
    if (!selectedAccount) return;
    const account = selectedAccount;
    submit(event.currentTarget, data => request(`/commands/accounts/${account.id}/${data.get('operation')}`, 'POST', {
        amount: data.get('amount'), currency: account.currency
    }));
});

$('status-button').addEventListener('click', async () => {
    if (!selectedAccount) return;
    const account = selectedAccount;
    $('status-button').disabled = true;
    try {
        await request(`/commands/accounts/${account.id}/status`, 'PATCH', {
            status: account.status === 'ACTIVATED' ? 'SUSPENDED' : 'ACTIVATED'
        });
        message('Status command accepted.');
    } catch (error) {
        message(error.message, true);
        if (selectedAccount?.id === account.id) $('status-button').disabled = false;
    }
});

$('account-select').addEventListener('change', event => selectAccount(event.target.value).catch(error => message(error.message, true)));
$('refresh').addEventListener('click', () => loadAccounts().catch(error => message(error.message, true)));
window.addEventListener('beforeunload', () => stream?.close());
loadAccounts().catch(error => message(error.message, true));
