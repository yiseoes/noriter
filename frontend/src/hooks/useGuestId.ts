const STORAGE_KEY = 'noriter-guest-id';

function generateUUID(): string {
  return 'guest_' + crypto.randomUUID().replace(/-/g, '').slice(0, 16);
}

export function getGuestId(): string {
  let id = localStorage.getItem(STORAGE_KEY);
  if (!id) {
    id = generateUUID();
    localStorage.setItem(STORAGE_KEY, id);
  }
  return id;
}
