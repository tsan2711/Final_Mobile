const toPlainObject = (doc) => {
  if (!doc) return null;
  if (typeof doc.toObject === 'function') {
    return doc.toObject({ virtuals: true });
  }
  return doc;
};

const formatUser = (user) => {
  const plain = toPlainObject(user);
  if (!plain) return null;

  return {
    id: (plain._id && typeof plain._id.toString === 'function') ? plain._id.toString() : (plain.id || null),
    email: plain.email ? String(plain.email) : '',
    full_name: plain.fullName ? String(plain.fullName) : (plain.full_name ? String(plain.full_name) : ''),
    phone: plain.phone ? String(plain.phone) : '',
    address: plain.address ? String(plain.address) : '',
    customer_type: plain.customerType ? String(plain.customerType) : (plain.customer_type ? String(plain.customer_type) : 'CUSTOMER'),
    is_active: plain.isActive !== undefined ? Boolean(plain.isActive) : true,
    email_verified: plain.emailVerified !== undefined ? Boolean(plain.emailVerified) : false,
    phone_verified: plain.phoneVerified !== undefined ? Boolean(plain.phoneVerified) : false,
    last_login: plain.lastLogin || null,
    created_at: plain.createdAt || null,
    updated_at: plain.updatedAt || null
  };
};

const formatAccount = (account) => {
  const plain = toPlainObject(account);
  if (!plain) return null;

  return {
    id: (plain._id && typeof plain._id.toString === 'function') ? plain._id.toString() : (plain.id || null),
    user_id: (plain.userId && typeof plain.userId.toString === 'function') ? plain.userId.toString() : (plain.user_id ? String(plain.user_id) : null),
    account_number: plain.accountNumber ? String(plain.accountNumber) : (plain.account_number ? String(plain.account_number) : ''),
    masked_account_number: (account && account.maskedAccountNumber) ? String(account.maskedAccountNumber) : (plain.masked_account_number ? String(plain.masked_account_number) : ''),
    account_type: plain.accountType ? String(plain.accountType) : (plain.account_type ? String(plain.account_type) : ''),
    balance: (plain.balance !== null && plain.balance !== undefined) ? Number(plain.balance) : 0,
    formatted_balance: (account && account.formattedBalance) ? String(account.formattedBalance) : (plain.formatted_balance ? String(plain.formatted_balance) : ''),
    interest_rate: (plain.interestRate !== null && plain.interestRate !== undefined) ? Number(plain.interestRate) : ((plain.interest_rate !== null && plain.interest_rate !== undefined) ? Number(plain.interest_rate) : null),
    currency: plain.currency ? String(plain.currency) : 'VND',
    is_active: plain.isActive !== undefined ? Boolean(plain.isActive) : true,
    created_at: plain.createdAt || null,
    updated_at: plain.updatedAt || null
  };
};

const formatTransaction = (transaction) => {
  const plain = toPlainObject(transaction);
  if (!plain) return null;

  const fromAccount = plain.fromAccountId;
  const toAccount = plain.toAccountId;

  const fromAccountId = fromAccount?.id || fromAccount?._id || fromAccount;
  const toAccountId = toAccount?.id || toAccount?._id || toAccount;

  const normalizeAccountRef = (acc, fallbackNumber) => {
    if (!acc) return null;
    const accPlain = toPlainObject(acc);
    return {
      id: accPlain?._id?.toString?.() || accPlain?.id || null,
      account_number: accPlain?.accountNumber || fallbackNumber || '',
      account_type: accPlain?.accountType || '',
      masked_account_number: acc?.maskedAccountNumber || accPlain?.masked_account_number || '',
      formatted_balance: acc?.formattedBalance || accPlain?.formatted_balance || ''
    };
  };

  return {
    id: plain._id?.toString?.() || plain.id || null,
    transaction_id: plain.transactionId || '',
    from_account_id: fromAccountId ? fromAccountId.toString() : null,
    to_account_id: toAccountId ? toAccountId.toString() : null,
    from_account_number: plain.fromAccountNumber || fromAccount?.accountNumber || '',
    to_account_number: plain.toAccountNumber || toAccount?.accountNumber || '',
    amount: plain.amount ?? 0,
    formatted_amount: transaction?.formattedAmount || plain.formatted_amount || '',
    currency: plain.currency || 'VND',
    fee: plain.fee ?? 0,
    formatted_fee: plain.fee !== undefined ? `${plain.fee.toLocaleString?.() ?? plain.fee} ${plain.currency || 'VND'}` : '',
    total_amount: plain.totalAmount ?? 0,
    formatted_total_amount: transaction?.formattedTotalAmount || plain.formatted_total_amount || '',
    description: plain.description || '',
    transaction_type: plain.transactionType || plain.transaction_type || '',
    status: plain.status || '',
    reference_number: plain.transactionId || plain.reference_number || '',
    otp_verified: plain.otpVerified ?? false,
    failure_reason: plain.failureReason || '',
    created_at: plain.createdAt || null,
    updated_at: plain.updatedAt || null,
    processed_at: plain.processedAt || null,
    initiated_by: plain.initiatedBy ? plain.initiatedBy.toString?.() || plain.initiatedBy : null,
    from_account: normalizeAccountRef(fromAccount, plain.fromAccountNumber),
    to_account: normalizeAccountRef(toAccount, plain.toAccountNumber)
  };
};

const formatUtility = (utility) => {
  const plain = toPlainObject(utility);
  if (!plain) return null;

  const account = plain.accountId;
  const accountPlain = toPlainObject(account);

  return {
    id: plain._id?.toString?.() || plain.id || null,
    transaction_id: plain.transactionId || '',
    user_id: plain.userId?.toString?.() || plain.user_id || null,
    account_id: accountPlain?._id?.toString?.() || plain.account_id || null,
    account_number: accountPlain?.accountNumber || '',
    service_type: plain.serviceType || '',
    provider: plain.provider || '',
    service_number: plain.serviceNumber || '',
    amount: plain.amount ?? 0,
    formatted_amount: utility?.formattedAmount || plain.formatted_amount || '',
    currency: plain.currency || 'VND',
    fee: plain.fee ?? 0,
    formatted_fee: plain.fee !== undefined ? `${plain.fee.toLocaleString?.() ?? plain.fee} ${plain.currency || 'VND'}` : '',
    total_amount: plain.totalAmount ?? 0,
    formatted_total_amount: utility?.formattedTotalAmount || plain.formatted_total_amount || '',
    status: plain.status || '',
    description: plain.description || '',
    reference_number: plain.referenceNumber || '',
    failure_reason: plain.failureReason || '',
    metadata: plain.metadata || {},
    created_at: plain.createdAt || null,
    updated_at: plain.updatedAt || null,
    processed_at: plain.processedAt || null
  };
};

module.exports = {
  formatUser,
  formatAccount,
  formatTransaction,
  formatUtility
};

