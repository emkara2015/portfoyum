<?php
$pageTitle = 'TEFAŞ Fonları Yönetimi';
require_once __DIR__ . '/supabase.php';
require_once __DIR__ . '/auth.php';

$client = new SupabaseClient();

// Handle Fund Price Edit
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';
    $csrf = $_POST['csrf_token'] ?? '';

    if (!verifyCsrfToken($csrf)) {
        setFlashMessage('danger', 'Güvenlik doğrulaması başarısız!');
        header('Location: tefas.php');
        exit;
    }

    if ($action === 'update_fund') {
        $symbol = strtoupper(trim($_POST['symbol'] ?? ''));
        $price = (float)($_POST['price'] ?? 0);
        $riskLevel = (int)($_POST['risk_level'] ?? 0);

        if ($symbol && $price > 0) {
            $res = $client->update('tefas_funds', [
                'price' => $price,
                'risk_level' => $riskLevel,
                'updated_at' => date('c'),
                'last_attempt_at' => date('c'),
                'last_success_at' => date('c'),
                'last_sync_status' => 'success',
                'last_error' => null,
                'consecutive_failures' => 0,
                'is_active' => true
            ], 'symbol=eq.' . $symbol);

            if ($res['error']) {
                setFlashMessage('danger', 'Fon güncellenirken hata oluştu: ' . $res['message']);
            } else {
                setFlashMessage('success', $symbol . ' fon fiyatı başarıyla güncellendi.');
            }
        }
        header('Location: tefas.php');
        exit;
    }
}

// URL Query Parameters
$search = trim($_GET['search'] ?? '');
$sort = $_GET['sort'] ?? 'symbol';
$order = strtolower($_GET['order'] ?? 'asc') === 'desc' ? 'desc' : 'asc';
$riskFilter = $_GET['risk_filter'] ?? 'all';
$statusFilter = $_GET['status'] ?? 'all';
if (!in_array($statusFilter, ['all', 'active', 'inactive', 'failed'], true)) {
    $statusFilter = 'all';
}
$page = max(1, (int)($_GET['page'] ?? 1));
$perPage = max(10, min(500, (int)($_GET['per_page'] ?? 50)));

// Allowed sort columns for security
$allowedSorts = ['symbol', 'name', 'price', 'risk_level', 'yield_1m', 'yield_3m', 'yield_6m', 'yield_1y', 'yield_ytd', 'updated_at', 'last_success_at', 'consecutive_failures'];
if (!in_array($sort, $allowedSorts)) {
    $sort = 'symbol';
}

// Build Query
$query = $client->from('tefas_funds');

// Text Search Filter
if ($search !== '') {
    $query->ilike('name', $search);
}

// Risk Level Filter
if ($riskFilter === 'low') {
    $query->lte('risk_level', 2);
} elseif ($riskFilter === 'medium') {
    $query->gte('risk_level', 3)->lte('risk_level', 4);
} elseif ($riskFilter === 'high') {
    $query->gte('risk_level', 5);
} elseif (is_numeric($riskFilter) && (int)$riskFilter >= 1 && (int)$riskFilter <= 7) {
    $query->eq('risk_level', (int)$riskFilter);
}

if ($statusFilter === 'active') {
    $query->eq('is_active', 'true');
} elseif ($statusFilter === 'inactive') {
    $query->eq('is_active', 'false');
} elseif ($statusFilter === 'failed') {
    $query->eq('last_sync_status', 'failed');
}

// Server-side Ordering across all 2802+ funds with NULLS LAST
$query->order($sort, $order, 'nullslast');

// Fetch Paginated Result
$paginated = $query->paginate($page, $perPage);
$funds = $paginated['data'];
$totalFunds = $paginated['total'];
$totalPages = $paginated['totalPages'];

// Katalog özeti; filtreli tablo sayısından bağımsız gösterilir.
$catalogTotal = $client->from('tefas_funds')->countExact();
$activeFunds = $client->from('tefas_funds')->eq('is_active', 'true')->countExact();
$inactiveFunds = $client->from('tefas_funds')->eq('is_active', 'false')->countExact();
$failedFunds = $client->from('tefas_funds')->eq('last_sync_status', 'failed')->countExact();

// Helper parameters for URLs
$allQueryParams = [
    'search' => $search,
    'sort' => $sort,
    'order' => $order,
    'risk_filter' => $riskFilter,
    'status' => $statusFilter,
    'per_page' => $perPage
];

function formatSyncDate($value) {
    if (empty($value)) return '-';
    $timestamp = strtotime($value);
    return $timestamp ? date('d.m.Y H:i', $timestamp) : '-';
}

function getSortUrl($column, $currentSort, $currentOrder, $params) {
    $newOrder = ($currentSort === $column && $currentOrder === 'asc') ? 'desc' : 'asc';
    $p = array_merge($params, [
        'sort' => $column,
        'order' => $newOrder,
        'page' => 1
    ]);
    return 'tefas.php?' . http_build_query($p);
}

function renderSortIcon($column, $currentSort, $currentOrder) {
    if ($currentSort !== $column) {
        return '<i class="fa-solid fa-sort text-slate-600 ml-1 opacity-50"></i>';
    }
    return $currentOrder === 'asc' 
        ? '<i class="fa-solid fa-sort-up text-indigo-400 ml-1"></i>' 
        : '<i class="fa-solid fa-sort-down text-indigo-400 ml-1"></i>';
}

$csrfToken = generateCsrfToken();
include __DIR__ . '/includes/header.php';
?>

<!-- Header & Quick Filters Banner -->
<div class="glass-card rounded-2xl p-5 mb-6">
    <div class="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 mb-4">
        <div>
            <h3 class="text-base font-bold text-white flex items-center gap-2">
                <i class="fa-solid fa-chart-pie text-amber-400"></i> TEFAŞ Yatırım Fonları Kataloğu (<?= number_format($totalFunds) ?> Fon)
            </h3>
            <p class="text-xs text-slate-400">Güncel katalog: <?= number_format($catalogTotal) ?> fon · Son senkron durumlarını buradan yönetin</p>
        </div>

        <!-- One-Click Quick Filter Buttons -->
        <div class="flex flex-wrap items-center gap-2">
            <span class="text-xs font-semibold text-slate-400 mr-1 hidden sm:inline">Hızlı Filtreler:</span>
            
            <a href="tefas.php?sort=yield_1m&order=desc" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition <?= ($sort === 'yield_1m' && $order === 'desc') ? 'bg-purple-500 text-white font-bold shadow-lg shadow-purple-500/20' : 'bg-slate-800 text-slate-300 hover:bg-slate-700' ?>">
                <i class="fa-solid fa-bolt text-purple-400 mr-1"></i> En Yüksek 1A
            </a>

            <a href="tefas.php?sort=yield_3m&order=desc" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition <?= ($sort === 'yield_3m' && $order === 'desc') ? 'bg-pink-500 text-white font-bold shadow-lg shadow-pink-500/20' : 'bg-slate-800 text-slate-300 hover:bg-slate-700' ?>">
                <i class="fa-solid fa-fire text-pink-400 mr-1"></i> En Yüksek 3A
            </a>

            <a href="tefas.php?sort=yield_1y&order=desc" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition <?= ($sort === 'yield_1y' && $order === 'desc') ? 'bg-emerald-500 text-slate-950 font-bold shadow-lg shadow-emerald-500/20' : 'bg-slate-800 text-slate-300 hover:bg-slate-700' ?>">
                <i class="fa-solid fa-trophy text-amber-400 mr-1"></i> En Yüksek 1Y
            </a>

            <a href="tefas.php?sort=yield_ytd&order=desc" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition <?= ($sort === 'yield_ytd' && $order === 'desc') ? 'bg-sky-500 text-slate-950 font-bold shadow-lg shadow-sky-500/20' : 'bg-slate-800 text-slate-300 hover:bg-slate-700' ?>">
                <i class="fa-solid fa-chart-line text-sky-400 mr-1"></i> En Yüksek YTD
            </a>

            <a href="tefas.php?risk_filter=low" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition <?= ($riskFilter === 'low') ? 'bg-indigo-600 text-white font-bold shadow-lg shadow-indigo-600/30' : 'bg-slate-800 text-slate-300 hover:bg-slate-700' ?>">
                <i class="fa-solid fa-shield text-indigo-400 mr-1"></i> Düşük Risk (1-2)
            </a>

            <?php if ($search !== '' || $sort !== 'symbol' || $order !== 'asc' || $riskFilter !== 'all' || $statusFilter !== 'all'): ?>
                <a href="tefas.php" class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-medium transition flex items-center gap-1">
                    <i class="fa-solid fa-rotate-left"></i> Sıfırla
                </a>
            <?php endif; ?>
        </div>
    </div>

    <!-- Search & Filter Selectors Toolbar -->
    <div class="pt-4 border-t border-[#1f293d] flex flex-col md:flex-row items-center justify-between gap-4">
        <!-- Search Form -->
        <form method="GET" action="tefas.php" class="w-full md:w-80 relative">
            <input type="hidden" name="sort" value="<?= htmlspecialchars($sort) ?>">
            <input type="hidden" name="order" value="<?= htmlspecialchars($order) ?>">
            <input type="hidden" name="risk_filter" value="<?= htmlspecialchars($riskFilter) ?>">
            <input type="hidden" name="status" value="<?= htmlspecialchars($statusFilter) ?>">
            <input type="hidden" name="per_page" value="<?= $perPage ?>">
            
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400 text-xs">
                <i class="fa-solid fa-magnifying-glass"></i>
            </div>
            <input type="text" name="search" value="<?= htmlspecialchars($search) ?>" placeholder="Fon adı veya kural koda göre ara..."
                class="w-full pl-9 pr-4 py-2 bg-slate-900/80 border border-slate-700/80 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition">
        </form>

        <div class="flex flex-wrap items-center gap-3 w-full md:w-auto justify-end">
            <!-- Risk Filter Dropdown -->
            <select onchange="location.href='tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => 1])) ?>&risk_filter='+this.value" class="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-indigo-500">
                <option value="all" <?= $riskFilter === 'all' ? 'selected' : '' ?>>Tüm Risk Seviyeleri</option>
                <option value="low" <?= $riskFilter === 'low' ? 'selected' : '' ?>>Düşük Risk (1 - 2)</option>
                <option value="medium" <?= $riskFilter === 'medium' ? 'selected' : '' ?>>Orta Risk (3 - 4)</option>
                <option value="high" <?= $riskFilter === 'high' ? 'selected' : '' ?>>Yüksek Risk (5 - 7)</option>
                <option value="1" <?= $riskFilter === '1' ? 'selected' : '' ?>>Risk Seviyesi 1</option>
                <option value="2" <?= $riskFilter === '2' ? 'selected' : '' ?>>Risk Seviyesi 2</option>
                <option value="3" <?= $riskFilter === '3' ? 'selected' : '' ?>>Risk Seviyesi 3</option>
                <option value="4" <?= $riskFilter === '4' ? 'selected' : '' ?>>Risk Seviyesi 4</option>
                <option value="5" <?= $riskFilter === '5' ? 'selected' : '' ?>>Risk Seviyesi 5</option>
                <option value="6" <?= $riskFilter === '6' ? 'selected' : '' ?>>Risk Seviyesi 6</option>
                <option value="7" <?= $riskFilter === '7' ? 'selected' : '' ?>>Risk Seviyesi 7</option>
            </select>

            <select onchange="location.href='tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => 1])) ?>&status='+this.value" class="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-indigo-500">
                <option value="all" <?= $statusFilter === 'all' ? 'selected' : '' ?>>Tüm Durumlar</option>
                <option value="active" <?= $statusFilter === 'active' ? 'selected' : '' ?>>Aktif Fonlar</option>
                <option value="inactive" <?= $statusFilter === 'inactive' ? 'selected' : '' ?>>Pasif Fonlar</option>
                <option value="failed" <?= $statusFilter === 'failed' ? 'selected' : '' ?>>Senkron Hatalı</option>
            </select>

            <!-- Items Per Page -->
            <select onchange="location.href='tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => 1])) ?>&per_page='+this.value" class="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-indigo-500">
                <option value="50" <?= $perPage == 50 ? 'selected' : '' ?>>50 / Sayfa</option>
                <option value="100" <?= $perPage == 100 ? 'selected' : '' ?>>100 / Sayfa</option>
                <option value="200" <?= $perPage == 200 ? 'selected' : '' ?>>200 / Sayfa</option>
                <option value="500" <?= $perPage == 500 ? 'selected' : '' ?>>500 / Sayfa</option>
            </select>
        </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-4">
        <div class="rounded-xl border border-slate-700/70 bg-slate-900/40 px-3 py-2">
            <div class="text-[10px] uppercase tracking-wider text-slate-500">Toplam Fon</div>
            <div class="mt-1 text-lg font-bold text-white"><?= number_format($catalogTotal) ?></div>
        </div>
        <div class="rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-wider text-emerald-400/70">Aktif</div>
            <div class="mt-1 text-lg font-bold text-emerald-300"><?= number_format($activeFunds) ?></div>
        </div>
        <div class="rounded-xl border border-slate-600/70 bg-slate-700/20 px-3 py-2">
            <div class="text-[10px] uppercase tracking-wider text-slate-400">Pasif</div>
            <div class="mt-1 text-lg font-bold text-slate-200"><?= number_format($inactiveFunds) ?></div>
        </div>
        <div class="rounded-xl border border-amber-500/20 bg-amber-500/5 px-3 py-2">
            <div class="text-[10px] uppercase tracking-wider text-amber-400/80">Son Senkron Hatası</div>
            <div class="mt-1 text-lg font-bold text-amber-300"><?= number_format($failedFunds) ?></div>
        </div>
    </div>
</div>

<!-- Interactive Funds Table -->
<div class="glass-card rounded-2xl p-6">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-xs">
            <thead>
                <tr class="border-b border-[#1f293d] text-slate-400 font-semibold uppercase tracking-wider select-none">
                    <!-- Column Header: Symbol -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('symbol', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            Fon Kodu <?= renderSortIcon('symbol', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Name -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('name', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            Fon Adı <?= renderSortIcon('name', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Price -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('price', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            Fiyat (TL) <?= renderSortIcon('price', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Risk Level -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('risk_level', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            Risk <?= renderSortIcon('risk_level', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Yield 1M -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('yield_1m', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            1A Getiri <?= renderSortIcon('yield_1m', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Yield 3M -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('yield_3m', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            3A Getiri <?= renderSortIcon('yield_3m', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Yield 6M -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('yield_6m', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            6A Getiri <?= renderSortIcon('yield_6m', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Yield 1Y -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('yield_1y', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            1Y Getiri <?= renderSortIcon('yield_1y', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Yield YTD -->
                    <th class="pb-3 px-3">
                        <a href="<?= getSortUrl('yield_ytd', $sort, $order, $allQueryParams) ?>" class="flex items-center hover:text-white transition">
                            YTD Getiri <?= renderSortIcon('yield_ytd', $sort, $order) ?>
                        </a>
                    </th>

                    <!-- Column Header: Sync Status -->
                    <th class="pb-3 px-3">Senkron</th>

                    <th class="pb-3 px-3 text-right">İşlem</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-[#1f293d]/50 text-slate-300">
                <?php if (empty($funds)): ?>
                    <tr>
                        <td colspan="11" class="py-12 text-center text-slate-500">
                            <i class="fa-solid fa-folder-open text-3xl mb-3 block text-slate-600"></i>
                            Kriterlere uygun TEFAŞ fonu bulunamadı.
                        </td>
                    </tr>
                <?php else: ?>
                    <?php foreach ($funds as $f): ?>
                        <tr class="hover:bg-slate-800/40 transition">
                            <td class="py-3.5 px-3">
                                <span class="px-2.5 py-1 rounded bg-amber-500/10 text-amber-400 font-bold border border-amber-500/20 font-mono text-xs">
                                    <?= htmlspecialchars($f['symbol']) ?>
                                </span>
                            </td>

                            <td class="py-3.5 px-3">
                                <div class="font-medium text-slate-200"><?= htmlspecialchars($f['name']) ?></div>
                                <div class="text-[10px] text-slate-500"><?= htmlspecialchars($f['management_company'] ?? 'Portföy Yönetimi') ?></div>
                            </td>

                            <td class="py-3.5 px-3 font-mono font-bold text-emerald-400">
                                <?php if (isset($f['price']) && (float)$f['price'] > 0): ?>
                                    ₺<?= number_format((float)$f['price'], 6) ?>
                                <?php else: ?>
                                    <span class="text-slate-500">-</span>
                                <?php endif; ?>
                            </td>

                            <td class="py-3.5 px-3">
                                <?php $risk = (int)($f['risk_level'] ?? 1); ?>
                                <span class="px-2 py-0.5 rounded text-[10px] font-bold <?= $risk >= 5 ? 'bg-red-500/10 text-red-400 border border-red-500/20' : ($risk >= 3 ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : 'bg-blue-500/10 text-blue-400 border border-blue-500/20') ?>">
                                    <?= $risk ?> / 7
                                </span>
                            </td>

                            <!-- 1M Yield -->
                            <td class="py-3.5 px-3 font-mono">
                                <?php if ($f['yield_1m'] === null || $f['yield_1m'] === ''): ?>
                                    <span class="text-slate-500 font-mono text-[11px]">-</span>
                                <?php else: ?>
                                    <?php $y1m = (float)$f['yield_1m']; ?>
                                    <span class="font-semibold <?= $y1m >= 0 ? 'text-emerald-400' : 'text-red-400' ?>">
                                        <?= $y1m >= 0 ? '+' : '' ?><?= number_format($y1m, 2) ?>%
                                    </span>
                                <?php endif; ?>
                            </td>

                            <!-- 3M Yield -->
                            <td class="py-3.5 px-3 font-mono">
                                <?php if ($f['yield_3m'] === null || $f['yield_3m'] === ''): ?>
                                    <span class="text-slate-500 font-mono text-[11px]">-</span>
                                <?php else: ?>
                                    <?php $y3m = (float)$f['yield_3m']; ?>
                                    <span class="font-semibold <?= $y3m >= 0 ? 'text-emerald-400' : 'text-red-400' ?>">
                                        <?= $y3m >= 0 ? '+' : '' ?><?= number_format($y3m, 2) ?>%
                                    </span>
                                <?php endif; ?>
                            </td>

                            <!-- 6M Yield -->
                            <td class="py-3.5 px-3 font-mono">
                                <?php if ($f['yield_6m'] === null || $f['yield_6m'] === ''): ?>
                                    <span class="text-slate-500 font-mono text-[11px]">-</span>
                                <?php else: ?>
                                    <?php $y6m = (float)$f['yield_6m']; ?>
                                    <span class="font-semibold <?= $y6m >= 0 ? 'text-emerald-400' : 'text-red-400' ?>">
                                        <?= $y6m >= 0 ? '+' : '' ?><?= number_format($y6m, 2) ?>%
                                    </span>
                                <?php endif; ?>
                            </td>

                            <!-- 1Y Yield -->
                            <td class="py-3.5 px-3 font-mono">
                                <?php if ($f['yield_1y'] === null || $f['yield_1y'] === ''): ?>
                                    <span class="text-slate-500 font-mono text-[11px]">-</span>
                                <?php else: ?>
                                    <?php $y1 = (float)$f['yield_1y']; ?>
                                    <span class="font-semibold <?= $y1 >= 0 ? 'text-emerald-400' : 'text-red-400' ?>">
                                        <?= $y1 >= 0 ? '+' : '' ?><?= number_format($y1, 2) ?>%
                                    </span>
                                <?php endif; ?>
                            </td>

                            <!-- YTD Yield -->
                            <td class="py-3.5 px-3 font-mono">
                                <?php if ($f['yield_ytd'] === null || $f['yield_ytd'] === ''): ?>
                                    <span class="text-slate-500 font-mono text-[11px]">-</span>
                                <?php else: ?>
                                    <?php $ytd = (float)$f['yield_ytd']; ?>
                                    <span class="font-semibold <?= $ytd >= 0 ? 'text-emerald-400' : 'text-red-400' ?>">
                                        <?= $ytd >= 0 ? '+' : '' ?><?= number_format($ytd, 2) ?>%
                                    </span>
                                <?php endif; ?>
                            </td>

                            <?php
                            $isActive = filter_var($f['is_active'] ?? true, FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
                            $isActive = $isActive !== false;
                            $syncStatus = $f['last_sync_status'] ?? 'unknown';
                            $failureCount = (int)($f['consecutive_failures'] ?? 0);
                            $hasPrice = isset($f['price']) && (float)$f['price'] > 0;
                            ?>
                            <td class="py-3.5 px-3 min-w-[170px]">
                                <?php if (!$isActive): ?>
                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-slate-700/60 text-slate-300 border border-slate-600 text-[10px] font-bold">
                                        <i class="fa-solid fa-box-archive"></i> Pasif
                                    </span>
                                <?php elseif ($syncStatus === 'failed'): ?>
                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20 text-[10px] font-bold">
                                        <i class="fa-solid fa-clock-rotate-left"></i> <?= $hasPrice ? 'Son değer korunuyor' : 'Veri alınamadı' ?>
                                    </span>
                                <?php elseif ($syncStatus === 'success'): ?>
                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 text-[10px] font-bold">
                                        <i class="fa-solid fa-circle-check"></i> Güncel
                                    </span>
                                <?php else: ?>
                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700 text-[10px] font-bold">
                                        <i class="fa-solid fa-hourglass-half"></i> Bekliyor
                                    </span>
                                <?php endif; ?>
                                <div class="mt-1 text-[10px] text-slate-500">
                                    Son başarılı: <?= htmlspecialchars(formatSyncDate($f['last_success_at'] ?? ($f['updated_at'] ?? null))) ?>
                                </div>
                                <div class="text-[10px] text-slate-600">
                                    Son deneme: <?= htmlspecialchars(formatSyncDate($f['last_attempt_at'] ?? null)) ?>
                                </div>
                                <?php if ($failureCount > 0): ?>
                                    <div class="text-[10px] text-amber-400/80">Ardışık hata: <?= number_format($failureCount) ?></div>
                                <?php endif; ?>
                            </td>

                            <td class="py-3.5 px-3 text-right">
                                <button onclick="openEditFundModal('<?= htmlspecialchars($f['symbol']) ?>', '<?= htmlspecialchars(addslashes($f['name'])) ?>', <?= (float)($f['price'] ?? 0) ?>, <?= (int)($f['risk_level'] ?? 1) ?>)"
                                    class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white font-medium transition text-xs">
                                    <i class="fa-solid fa-pen text-[10px] mr-1"></i> Düzenle
                                </button>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
    </div>

    <!-- Pagination Controls -->
    <?php if ($totalPages > 1): ?>
        <div class="mt-6 pt-4 border-t border-[#1f293d] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
            <div class="text-slate-400">
                Toplam <strong class="text-slate-200"><?= number_format($totalFunds) ?></strong> fondan 
                <strong class="text-slate-200"><?= number_format(($page - 1) * $perPage + 1) ?> - <?= number_format(min($totalFunds, $page * $perPage)) ?></strong> 
                arası gösteriliyor (Sayfa <strong class="text-slate-200"><?= $page ?> / <?= $totalPages ?></strong>)
            </div>

            <div class="flex items-center gap-1.5 flex-wrap">
                <!-- Previous Button -->
                <?php if ($page > 1): ?>
                    <a href="tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => $page - 1])) ?>" 
                       class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium transition flex items-center gap-1">
                        <i class="fa-solid fa-chevron-left text-[10px]"></i> Önceki
                    </a>
                <?php else: ?>
                    <span class="px-3 py-1.5 rounded-lg bg-slate-900 text-slate-600 font-medium cursor-not-allowed">
                        <i class="fa-solid fa-chevron-left text-[10px]"></i> Önceki
                    </span>
                <?php endif; ?>

                <!-- Numeric Page Links -->
                <?php
                $startPage = max(1, $page - 2);
                $endPage = min($totalPages, $page + 2);
                for ($p = $startPage; $p <= $endPage; $p++):
                ?>
                    <a href="tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => $p])) ?>" 
                       class="w-8 h-8 rounded-lg flex items-center justify-center font-medium transition <?= $p === $page ? 'bg-indigo-600 text-white font-bold shadow-lg shadow-indigo-600/30' : 'bg-slate-800 text-slate-400 hover:text-white' ?>">
                        <?= $p ?>
                    </a>
                <?php endfor; ?>

                <!-- Next Button -->
                <?php if ($page < $totalPages): ?>
                    <a href="tefas.php?<?= http_build_query(array_merge($allQueryParams, ['page' => $page + 1])) ?>" 
                       class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium transition flex items-center gap-1">
                        Sonraki <i class="fa-solid fa-chevron-right text-[10px]"></i>
                    </a>
                <?php else: ?>
                    <span class="px-3 py-1.5 rounded-lg bg-slate-900 text-slate-600 font-medium cursor-not-allowed">
                        Sonraki <i class="fa-solid fa-chevron-right text-[10px]"></i>
                    </span>
                <?php endif; ?>
            </div>
        </div>
    <?php endif; ?>
</div>

<!-- Modal: Edit Fund -->
<div id="editFundModal" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 hidden">
    <div class="glass-card w-full max-w-md rounded-2xl p-6 relative border border-slate-700">
        <button onclick="closeModal('editFundModal')" class="absolute top-4 right-4 text-slate-400 hover:text-white">
            <i class="fa-solid fa-xmark text-lg"></i>
        </button>

        <div class="flex items-center gap-3 mb-5">
            <div class="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 font-bold font-mono" id="editFundModalSymbol">
                FON
            </div>
            <div>
                <h3 class="text-base font-bold text-white">Fon Fiyatını Düzenle</h3>
                <p class="text-xs text-slate-400" id="editFundModalName">Fon Adı</p>
            </div>
        </div>

        <form method="POST" action="tefas.php" class="space-y-4">
            <input type="hidden" name="csrf_token" value="<?= $csrfToken ?>">
            <input type="hidden" name="action" value="update_fund">
            <input type="hidden" name="symbol" id="editFundInputSymbol" value="">

            <div>
                <label class="block text-xs font-semibold uppercase text-slate-300 mb-1.5">Güncel Fiyat (₺)</label>
                <input type="number" step="0.000001" name="price" id="editFundInputPrice" required
                    class="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-indigo-500 font-mono">
            </div>

            <div>
                <label class="block text-xs font-semibold uppercase text-slate-300 mb-1.5">Risk Düzeyi (1 - 7)</label>
                <input type="number" min="1" max="7" name="risk_level" id="editFundInputRisk" required
                    class="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-indigo-500 font-mono">
            </div>

            <div class="pt-2 flex justify-end gap-3">
                <button type="button" onclick="closeModal('editFundModal')" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium">İptal</button>
                <button type="submit" class="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/30">Güncelle</button>
            </div>
        </form>
    </div>
</div>

<script>
function openEditFundModal(symbol, name, price, risk) {
    document.getElementById('editFundModalSymbol').innerText = symbol;
    document.getElementById('editFundModalName').innerText = name;
    document.getElementById('editFundInputSymbol').value = symbol;
    document.getElementById('editFundInputPrice').value = price;
    document.getElementById('editFundInputRisk').value = risk;
    openModal('editFundModal');
}
</script>

<?php include __DIR__ . '/includes/footer.php'; ?>
