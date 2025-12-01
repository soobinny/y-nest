import {useEffect, useMemo, useRef, useState} from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";
import {Bar, BarChart, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis,} from "recharts";
import FavoriteStar from "../components/FavoriteStar";
import {useLocation} from "react-router-dom";

const LOAN_TYPE_CONFIG = [
  { type: "MORTGAGE_LOAN", title: "주택담보대출" },
  { type: "RENT_HOUSE_LOAN", title: "전세자금대출" },
  { type: "CREDIT_LOAN", title: "개인신용대출" },
];

const LOAN_PAGE_SIZE = 10;
const PRODUCT_PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 9;

const BANK_OPTIONS = [
  "국민은행",
  "신한은행",
  "우리은행",
  "하나은행",
  "농협은행",
  "IBK기업은행",
  "부산은행",
  "대구은행",
  "광주은행",
  "제주은행",
  "전북은행",
  "경남은행",
  "그 외",
];

const ETC_BANK_LABEL = "그 외";

const DEFAULT_MIN_RATE = 0;
const DEFAULT_MAX_RATE = 10;

const normalizeComparableText = (value) =>
  typeof value === "string" ? value.replace(/\s+/g, "").toLowerCase() : "";

const NORMALIZED_KNOWN_BANKS = BANK_OPTIONS.filter(
  (bank) => bank !== ETC_BANK_LABEL
).map((bank) => normalizeComparableText(bank));

const collectLoanProviderCandidates = (item = {}) => {
  const candidates = [
    item.companyName,
    item.korCoNm,
    item.bankName,
    item.bank,
    item.provider,
    item.providerName,
    item.finCoNm,
    item.finCoName,
    item.finInstNm,
    item.finInstName,
    item.kor_co_nm,
    item.fin_co_nm,
  ];
  const unique = new Set();
  candidates.forEach((candidate) => {
    if (typeof candidate === "string") {
      const trimmed = candidate.trim();
      if (trimmed) {
        unique.add(trimmed);
      }
    }
  });
  return Array.from(unique);
};

const INITIAL_LOAN_RESULTS = LOAN_TYPE_CONFIG.reduce((acc, cur) => {
  acc[cur.type] = [];
  return acc;
}, {});

const INITIAL_LOAN_PAGES = LOAN_TYPE_CONFIG.reduce((acc, cur) => {
  acc[cur.type] = 1;
  return acc;
}, {});

const formatRate = (value) => {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return "-";
  return num.toFixed(2).replace(/\.?0+$/, "");
};

const formatRateWithUnit = (value) => {
  const formatted = formatRate(value);
  return formatted === "-" ? "-" : `${formatted}%`;
};

const buildLoanFields = (type, item) => {
  switch (type) {
    case "MORTGAGE_LOAN":
      return [
        { label: "최저 금리", value: formatRateWithUnit(item.lendRateMin) },
        { label: "최고 금리", value: formatRateWithUnit(item.lendRateMax) },
        { label: "평균 금리", value: formatRateWithUnit(item.lendRateAvg) },
        { label: "금리 유형", value: item.lendTypeName || "-" },
        { label: "상환 방식", value: item.rpayTypeName || "-" },
        { label: "담보 유형", value: item.mrtgTypeName || "-" },
      ];
    case "RENT_HOUSE_LOAN":
      return [
        { label: "최저 금리", value: formatRateWithUnit(item.lendRateMin) },
        { label: "최고 금리", value: formatRateWithUnit(item.lendRateMax) },
        { label: "평균 금리", value: formatRateWithUnit(item.lendRateAvg) },
        { label: "금리 유형", value: item.lendTypeName || "-" },
        { label: "상환 방식", value: item.rpayTypeName || "-" },
      ];
    case "CREDIT_LOAN":
      return [
        { label: "평균 금리", value: formatRateWithUnit(item.crdtGradAvg) },
        { label: "금리 유형", value: item.crdtLendRateTypeNm || "-" },
        { label: "1등급", value: formatRateWithUnit(item.crdtGrad1) },
        { label: "4등급", value: formatRateWithUnit(item.crdtGrad4) },
        { label: "5등급", value: formatRateWithUnit(item.crdtGrad5) },
        { label: "6등급", value: formatRateWithUnit(item.crdtGrad6) },
        { label: "10등급", value: formatRateWithUnit(item.crdtGrad10) },
        { label: "11등급", value: formatRateWithUnit(item.crdtGrad11) },
        { label: "12등급", value: formatRateWithUnit(item.crdtGrad12) },
        { label: "13등급", value: formatRateWithUnit(item.crdtGrad13) },
      ];
    default:
      return [];
  }
};

const groupLoanItemsByProduct = (items = []) => {
  const grouped = new Map();

  items.forEach((item) => {
    const key = `${item.productName || ""}::${item.companyName || ""}`;

    if (!grouped.has(key)) {
      grouped.set(key, {
        productId: item.productId || null,
        productName: item.productName || "-",
        companyName: item.companyName || "-",
        variants: [],
      });
    }

    const group = grouped.get(key);

    // 혹시 첫 번째 옵션에는 productId가 없고 뒤에만 들어있을 수 있으니 보정
    if (!group.productId && item.productId) {
      group.productId = item.productId;
    }

    group.variants.push(item);
  });

  return Array.from(grouped.values());
};

const buildPageNumbers = (currentPage, totalPages) => {
  const safeTotal = Math.max(1, totalPages || 1);
  const safeCurrent = Math.max(1, Math.min(currentPage || 1, safeTotal));
  if (safeTotal <= MAX_PAGE_BUTTONS) {
    return Array.from({ length: safeTotal }, (_, idx) => idx + 1);
  }
  const blockIndex = Math.floor((safeCurrent - 1) / MAX_PAGE_BUTTONS);
  const start = blockIndex * MAX_PAGE_BUTTONS + 1;
  const end = Math.min(safeTotal, start + MAX_PAGE_BUTTONS - 1);
  return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
};

const toNumberOrNull = (value) => {
  if (value === null || value === undefined) return null;
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  const str = String(value);
  const match = str.match(/-?\d+(\.\d+)?/);
  if (!match) return null;
  const num = Number(match[0]);
  return Number.isFinite(num) ? num : null;
};

const getLoanVariantComparableRate = (variant = {}) => {
  const candidates = [
    variant.lendRateAvg,
    variant.crdtGradAvg,
    variant.lendRateMin,
    variant.lendRateMax,
    variant.crdtGrad1,
    variant.crdtGrad4,
    variant.crdtGrad5,
    variant.crdtGrad6,
    variant.crdtGrad10,
    variant.crdtGrad11,
    variant.crdtGrad12,
    variant.crdtGrad13,
  ];

  for (const candidate of candidates) {
    const parsed = toNumberOrNull(candidate);
    if (parsed !== null) {
      return parsed;
    }
  }
  return null;
};

const computeLoanGroupRate = (group) => {
  if (!group || !Array.isArray(group.variants)) return null;
  const rates = group.variants
    .map(getLoanVariantComparableRate)
    .filter((rate) => rate !== null);
  if (!rates.length) return null;
  return Math.min(...rates);
};

const sortLoanGroups = (groups = [], sortOption = "") => {
  if (!Array.isArray(groups) || groups.length <= 1) {
    return groups;
  }

  const [rawKey = "", rawDirection = "asc"] = sortOption.split(",");
  const key = rawKey.trim();
  const direction = rawDirection.trim().toLowerCase() === "desc" ? -1 : 1;
  if (!key) {
    return groups;
  }

  const normalized = [...groups];

  if (key === "productName" || key === "product_name") {
    normalized.sort((a, b) => {
      const nameA = (a?.productName || "").toString().toLowerCase();
      const nameB = (b?.productName || "").toString().toLowerCase();
      if (nameA === nameB) return 0;
      return nameA > nameB ? direction : -direction;
    });
    return normalized;
  }

  if (key === "interestRate" || key === "interest_rate") {
    normalized.sort((a, b) => {
      const rateA = computeLoanGroupRate(a);
      const rateB = computeLoanGroupRate(b);
      if (rateA === null && rateB === null) return 0;
      if (rateA === null) return 1;
      if (rateB === null) return -1;
      if (rateA === rateB) return 0;
      return rateA > rateB ? direction : -direction;
    });
    return normalized;
  }

  return groups;
};

const sortFinanceProducts = (items = [], sortOption = "") => {
  if (!Array.isArray(items) || items.length <= 1) {
    return items;
  }

  const [rawKey = "", rawDirection = "asc"] = sortOption.split(",");
  const key = rawKey.trim();
  const direction = rawDirection.trim().toLowerCase() === "desc" ? -1 : 1;
  if (!key) {
    return items;
  }

  const sorted = [...items];

  if (key === "productName" || key === "product_name") {
    sorted.sort((a, b) => {
      const nameA = (a?.productName || "").toString().toLowerCase();
      const nameB = (b?.productName || "").toString().toLowerCase();
      if (nameA === nameB) return 0;
      return nameA > nameB ? direction : -direction;
    });
    return sorted;
  }

  if (key === "interestRate" || key === "interest_rate") {
    sorted.sort((a, b) => {
      const rateA = toNumberOrNull(a?.interestRate);
      const rateB = toNumberOrNull(b?.interestRate);
      if (rateA === null && rateB === null) return 0;
      if (rateA === null) return 1;
      if (rateB === null) return -1;
      if (rateA === rateB) return 0;
      return rateA > rateB ? direction : -direction;
    });
    return sorted;
  }

  return sorted;
};

export default function FinancePage() {
  const [products, setProducts] = useState([]);
  const [productPage, setProductPage] = useState(1);
  const [totalProductPages, setTotalProductPages] = useState(1);
  const [totalProductCount, setTotalProductCount] = useState(0);
  const [loanResults, setLoanResults] = useState(() => ({
    ...INITIAL_LOAN_RESULTS,
  }));
  const [activeLoanType, setActiveLoanType] = useState(
    LOAN_TYPE_CONFIG[0].type
  );
  const [loanPageByType, setLoanPageByType] = useState({
    ...INITIAL_LOAN_PAGES,
  });
  const [loading, setLoading] = useState(true);
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const typeParam = params.get("type");

  const [category, setCategory] = useState(() => {
    if (typeParam === "saving") return "SAVING";
    if (typeParam === "loan") return "LOAN";
    return "DEPOSIT"; // 기본값
  });

    useEffect(() => {
    if (typeParam === "saving") setCategory("SAVING");
    else if (typeParam === "loan") setCategory("LOAN");
    else if (typeParam === "deposit") setCategory("DEPOSIT");
  }, [typeParam]);

  const [keyword, setKeyword] = useState("");
  const [sortOption, setSortOption] = useState("id,desc");
  const latestCategoryRef = useRef(category);

  // 대출 계산용 상태
  const [loanAmount, setLoanAmount] = useState("");
  const [loanRate, setLoanRate] = useState("");
  const [loanYears, setLoanYears] = useState("");
  const [repayType, setRepayType] = useState("equal_principal_interest");
  const [monthlyPayment, setMonthlyPayment] = useState(null);
  const [totalPayment, setTotalPayment] = useState(null);
  const [totalInterest, setTotalInterest] = useState(null);

  // 월 상환액 계산 함수
  const calculateLoan = () => {
    // 문자열 → 숫자 변환
    const amountNum = parseInt(loanAmount);
    const rateNum = parseInt(loanRate);
    const yearsNum = parseInt(loanYears);

    // 입력 검증
    if (
      !loanAmount.trim() ||
      !loanRate.trim() ||
      !loanYears.trim() ||
      isNaN(amountNum) ||
      isNaN(rateNum) ||
      isNaN(yearsNum) ||
      amountNum <= 0 ||
      rateNum <= 0 ||
      yearsNum <= 0
    ) {
      alert("모든 값을 올바르게 입력해 주세요.");
      return;
    }

    const principal = amountNum * 10000; // 만원 → 원 단위
    const months = yearsNum * 12;
    const monthlyRate = rateNum / 100 / 12;

    let monthlyPay = 0;
    let totalPay = 0;
    let totalInt = 0;

    if (monthlyRate === 0) {
      // 무이자일 경우 단순 분할
      monthlyPay = principal / months;
      totalPay = principal;
      totalInt = 0;
    } else if (repayType === "equal_principal_interest") {
      // 원리금균등상환
      monthlyPay =
        (principal * monthlyRate * Math.pow(1 + monthlyRate, months)) /
        (Math.pow(1 + monthlyRate, months) - 1);
      totalPay = monthlyPay * months;
      totalInt = totalPay - principal;
    } else {
      // 원금균등상환 — 매달 원금 일정, 이자는 남은 원금 기준
      const monthlyPrincipal = principal / months;
      let totalFirstMonth = 0;
      let totalPaid = 0;
      let totalInterestAcc = 0;

      for (let i = 0; i < months; i++) {
        const remainingPrincipal = principal - monthlyPrincipal * i;
        const interest = remainingPrincipal * monthlyRate;
        const payment = monthlyPrincipal + interest;
        totalPaid += payment;
        totalInterestAcc += interest;
        if (i === 0) totalFirstMonth = payment; // 첫 달 상환액
      }
      monthlyPay = totalFirstMonth;
      totalPay = totalPaid;
      totalInt = totalInterestAcc;
    }

    setMonthlyPayment(Math.round(monthlyPay));
    setTotalPayment(Math.round(totalPay));
    setTotalInterest(Math.round(totalInt));
  };

  // 새 필터 상태
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedBanks, setSelectedBanks] = useState([]);
  const [minRate, setMinRate] = useState(DEFAULT_MIN_RATE);
  const [maxRate, setMaxRate] = useState(DEFAULT_MAX_RATE);
  const isLoanCategory = category === "LOAN";
  const activeLoanGroups = useMemo(() => {
    if (!isLoanCategory) {
      return [];
    }
    const groups = groupLoanItemsByProduct(loanResults[activeLoanType] || []);
    return sortLoanGroups(groups, sortOption);
  }, [isLoanCategory, loanResults, activeLoanType, sortOption]);
  const activeLoanPage = loanPageByType[activeLoanType] || 1;
  const totalLoanPages = Math.max(
    1,
    Math.ceil(activeLoanGroups.length / LOAN_PAGE_SIZE) || 1
  );
  const paginatedLoanGroups = isLoanCategory
    ? activeLoanGroups.slice(
        (activeLoanPage - 1) * LOAN_PAGE_SIZE,
        activeLoanPage * LOAN_PAGE_SIZE
      )
    : [];
  const displayedLoanCount = isLoanCategory ? activeLoanGroups.length : 0;
  const activeLoanConfig = LOAN_TYPE_CONFIG.find(
    ({ type }) => type === activeLoanType
  );
  const visibleLoanConfigs =
    isLoanCategory && activeLoanConfig
      ? [
          {
            ...activeLoanConfig,
            groups: paginatedLoanGroups,
            totalGroups: activeLoanGroups.length,
            totalPages: totalLoanPages,
            page: activeLoanPage,
          },
        ]
      : [];

  useEffect(() => {
    if (isLoanCategory) return;
    const maxPage = Math.max(1, totalProductPages || 1);
    if (productPage > maxPage) {
      fetchProducts(maxPage);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoanCategory, productPage, totalProductPages]);

  useEffect(() => {
    fetchProducts(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [category, sortOption]);

  useEffect(() => {
    latestCategoryRef.current = category;
  }, [category]);

  useEffect(() => {
    if (category === "LOAN") {
      setActiveLoanType((prev) => {
        if (LOAN_TYPE_CONFIG.some(({ type }) => type === prev)) {
          return prev;
        }
        return LOAN_TYPE_CONFIG[0].type;
      });
    } else {
      setProductPage(1);
    }
  }, [category]);

  useEffect(() => {
    if (!isLoanCategory || !activeLoanConfig) return;

    setLoanPageByType((prev) => {
      const currentPage = prev[activeLoanType] || 1;
      const clampedPage = Math.min(currentPage, totalLoanPages);
      if (clampedPage === currentPage) {
        return prev;
      }
      return {
        ...prev,
        [activeLoanType]: clampedPage,
      };
    });
  }, [isLoanCategory, activeLoanType, totalLoanPages, activeLoanConfig]);

  const fetchProducts = async (requestedPage = productPage, overrides = {}) => {
    try {
      setLoading(true);
      const requestedCategory = category;

      const effectiveMinRate =
        overrides.minRate !== undefined ? overrides.minRate : minRate;
      const effectiveMaxRate =
        overrides.maxRate !== undefined ? overrides.maxRate : maxRate;
      const effectiveSelectedBanks =
        overrides.selectedBanks !== undefined
          ? overrides.selectedBanks
          : selectedBanks;

      const selectedNormalBanks = effectiveSelectedBanks
        .filter((bank) => bank !== ETC_BANK_LABEL)
        .map((bank) => bank.trim())
        .filter((bank) => bank.length > 0);

      const excludeProviders = effectiveSelectedBanks.includes(ETC_BANK_LABEL)
        ? BANK_OPTIONS.filter((bank) => bank !== ETC_BANK_LABEL).map((bank) =>
            bank.trim()
          )
        : [];

      const serializeParams = (query) => {
        const searchParams = new URLSearchParams();
        Object.entries(query).forEach(([key, value]) => {
          if (value === undefined || value === null || value === "") return;
          if (Array.isArray(value)) {
            value.forEach((item) => {
              if (item !== undefined && item !== null && item !== "") {
                searchParams.append(key, item);
              }
            });
          } else {
            searchParams.append(key, value);
          }
        });
        return searchParams.toString();
      };

      if (requestedCategory === "LOAN") {
        setLoanResults({ ...INITIAL_LOAN_RESULTS });

        const includeEtc = effectiveSelectedBanks.includes(ETC_BANK_LABEL);
        const normalizedSelectedBanks = selectedNormalBanks.map((bank) =>
          normalizeComparableText(bank)
        );
        const normalizedKeyword = (keyword || "").trim().toLowerCase();

        const safeMinRate = Number.isFinite(Number(effectiveMinRate))
          ? Number(effectiveMinRate)
          : DEFAULT_MIN_RATE;
        const safeMaxRate = Number.isFinite(Number(effectiveMaxRate))
          ? Number(effectiveMaxRate)
          : DEFAULT_MAX_RATE;
        const minRateBound = Math.min(safeMinRate, safeMaxRate);
        const maxRateBound = Math.max(safeMinRate, safeMaxRate);
        const hasCustomRateRange =
          minRateBound > DEFAULT_MIN_RATE || maxRateBound < DEFAULT_MAX_RATE;

        const responses = await Promise.allSettled(
          LOAN_TYPE_CONFIG.map(({ type }) =>
            api.get(`/finance/loans/options/type/${type}`)
          )
        );

        const matchesKeyword = (item) => {
          if (!normalizedKeyword) return true;
          const candidates = [
            item.productName,
            item.companyName,
            item.finPrdtNm,
            item.korCoNm,
          ];
          return candidates.some(
            (candidate) =>
              typeof candidate === "string" &&
              candidate.toLowerCase().includes(normalizedKeyword)
          );
        };

        const matchesBankSelection = (item) => {
          if (normalizedSelectedBanks.length === 0 && !includeEtc) {
            return true;
          }

          const providerCandidates = collectLoanProviderCandidates(item);
          const normalizedProviders = providerCandidates
            .map((value) => normalizeComparableText(value))
            .filter(Boolean);

          const matchesExplicitBanks = normalizedSelectedBanks.length
            ? normalizedSelectedBanks.some((bank) =>
                normalizedProviders.some((provider) => provider.includes(bank))
              )
            : true;

          if (!includeEtc) {
            return matchesExplicitBanks;
          }

          const isKnownBank = normalizedProviders.some((provider) =>
            NORMALIZED_KNOWN_BANKS.some((known) => provider.includes(known))
          );

          if (normalizedSelectedBanks.length === 0) {
            return !isKnownBank;
          }

          // Union of explicit selections and "기타" providers
          if (!isKnownBank) {
            return true;
          }

          return matchesExplicitBanks;
        };

        const matchesRateRange = (item) => {
          const rate = getLoanVariantComparableRate(item);
          if (rate === null) {
            return !hasCustomRateRange;
          }
          return rate >= minRateBound && rate <= maxRateBound;
        };

        const nextLoanResults = { ...INITIAL_LOAN_RESULTS };
        responses.forEach((result, index) => {
          const { type } = LOAN_TYPE_CONFIG[index];
          if (result.status === "fulfilled") {
            const options = result.value?.data || [];
            nextLoanResults[type] = options.filter(
              (item) =>
                matchesKeyword(item) &&
                matchesBankSelection(item) &&
                matchesRateRange(item)
            );
          } else {
            console.error(`Loan option fetch failed (${type}):`, result.reason);
          }
        });

        if (latestCategoryRef.current !== requestedCategory) {
          return;
        }

        setLoanResults(nextLoanResults);
        setLoanPageByType({ ...INITIAL_LOAN_PAGES });
        return;
      }

      const safePage = Math.max(1, requestedPage || 1);

      const params = {
        productType: requestedCategory,
        keyword,
        minRate: effectiveMinRate,
        maxRate: effectiveMaxRate,
        sort: sortOption,
        page: safePage - 1,
        size: PRODUCT_PAGE_SIZE,
      };

      if (selectedNormalBanks.length > 0) {
        params.providers = selectedNormalBanks;
      }

      if (excludeProviders.length > 0) {
        params.excludeProviders = excludeProviders;
      }

      const res = await api.get("/finance/products", {
        params,
        paramsSerializer: serializeParams,
      });

      if (latestCategoryRef.current !== requestedCategory) {
        return;
      }

      const rawProducts = res.data.content || [];
      const sortedProducts = sortFinanceProducts(rawProducts, sortOption);
      setProducts(sortedProducts);
      setTotalProductPages(res.data.totalPages ?? 1);
      setTotalProductCount(res.data.totalElements ?? rawProducts.length);
      setProductPage(safePage);
    } catch (err) {
      console.error("Failed to fetch finance products:", err);
      if (category === "LOAN") {
        setLoanResults({ ...INITIAL_LOAN_RESULTS });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleProductPageChange = (nextPage) => {
    const clamped = Math.max(1, Math.min(nextPage, totalProductPages || 1));
    if (clamped === productPage) return;
    fetchProducts(clamped);
  };

  const handleLoanSubTabClick = (type) => {
    setActiveLoanType(type);
    setLoanPageByType((prev) => ({
      ...prev,
      [type]: 1,
    }));
  };

  const handleLoanPageChange = (type, nextPage, totalPages) => {
    setLoanPageByType((prev) => {
      const current = prev[type] || 1;
      const clamped = Math.max(1, Math.min(nextPage, totalPages));
      if (clamped === current) {
        return prev;
      }
      return {
        ...prev,
        [type]: clamped,
      };
    });
  };

  const handleBankToggle = (bank, checked) => {
    setSelectedBanks((prev) =>
      checked ? [...prev, bank] : prev.filter((b) => b !== bank)
    );
  };

  const handleResetFilters = () => {
    setSelectedBanks([]);
    setMinRate(DEFAULT_MIN_RATE);
    setMaxRate(DEFAULT_MAX_RATE);
    setDropdownOpen(false);
    setProductPage(1);
    setLoanPageByType({ ...INITIAL_LOAN_PAGES });
    fetchProducts(1, {
      selectedBanks: [],
      minRate: DEFAULT_MIN_RATE,
      maxRate: DEFAULT_MAX_RATE,
    });
  };

  return (
    <AppLayout>
      <div style={styles.page}>
        <div style={styles.sidebarContainer}>
          {/* 왼쪽 필터 사이드바 */}
          <aside style={styles.sidebar}>
            <h3 style={styles.filterTitle}>조건 검색</h3>

            {/* 은행 선택 */}
            <div style={styles.filterGroup}>
              <div
                style={{ ...styles.filterLabel, cursor: "pointer" }}
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                은행 선택 ▾
              </div>

              {dropdownOpen && (
                <div style={styles.dropdownList}>
                  {BANK_OPTIONS.map((bank) => (
                    <label key={bank} style={styles.checkboxLabel}>
                      <input
                        type="checkbox"
                        checked={selectedBanks.includes(bank)}
                        onChange={(e) =>
                          handleBankToggle(bank, e.target.checked)
                        }
                      />
                      {bank}
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div style={styles.filterGroup}>
              <label style={styles.filterLabel}>금리 범위</label>
              <div
                style={{ display: "flex", alignItems: "center", gap: "10px" }}
              >
                <input
                  type="number"
                  value={minRate}
                  onChange={(e) => setMinRate(parseFloat(e.target.value) || 0)}
                  min={0}
                  max={maxRate}
                  step={0.1}
                  style={{
                    width: "50px",
                    height: "20px",
                    padding: "0 0px 0 13px",
                    border: "1px solid #ddd",
                    borderRadius: "6px",
                    textAlign: "center",
                  }}
                />
                <span>~</span>
                <input
                  type="number"
                  value={maxRate}
                  onChange={(e) => setMaxRate(parseFloat(e.target.value) || 0)}
                  min={minRate}
                  max={10}
                  step={0.1}
                  style={{
                    width: "50px",
                    height: "20px",
                    padding: "0 0px 0 13px",
                    border: "1px solid #ddd",
                    borderRadius: "6px",
                    textAlign: "center",
                  }}
                />
                <span>%</span>
              </div>
              <p style={styles.rateText}>
                금리 {minRate}% ~ {maxRate}% 사이 상품 보기
              </p>
            </div>

            {/* 검색 버튼 */}
            <button
              onClick={() => fetchProducts(1)}
              style={styles.filterButton}
            >
              선택된 조건 검색하기
            </button>
            <button
              type="button"
              onClick={handleResetFilters}
              style={styles.resetButton}
            >
              조건 초기화
            </button>
          </aside>

          {/* 대출 금리 계산 박스 (대출 탭에서만 표시) */}
          {isLoanCategory && (
            <div style={styles.sidebar}>
              <h3 style={styles.filterTitle}>대출 금리 계산</h3>

              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                <label style={styles.filterLabel}>
                  대출 금액 (만원)
                  <input
                    type="text"
                    value={loanAmount}
                    onChange={(e) => {
                      const v = e.target.value.replace(/[^0-9]/g, "");
                      setLoanAmount(v);
                    }}
                    style={styles.inputBox}
                  />
                </label>

                <label style={styles.filterLabel}>
                  연 이자율 (%)
                  <input
                    type="text"
                    value={loanRate}
                    onChange={(e) => {
                      const v = e.target.value.replace(/[^0-9.]/g, "");
                      if ((v.match(/\./g) || []).length > 1) return; // 소수점 2개 방지
                      setLoanRate(v);
                    }}
                    style={styles.inputBox}
                  />
                </label>

                <label style={styles.filterLabel}>
                  상환 기간 (년)
                  <input
                    type="text"
                    value={loanYears}
                    onChange={(e) => {
                      const v = e.target.value.replace(/[^0-9]/g, "");
                      setLoanYears(v);
                    }}
                    style={styles.inputBox}
                  />
                </label>

                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "8px 10px",
                    marginTop: "6px",
                    width: "100%",
                    overflow: "hidden",
                  }}
                >
                  <label
                    style={{
                      whiteSpace: "nowrap",
                      flex: "1 1 45%",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      fontSize: "14px",
                      fontWeight: "600",
                      color: "#333",
                    }}
                  >
                    <input
                      type="radio"
                      name="repayType"
                      value="equal_principal_interest"
                      checked={repayType === "equal_principal_interest"}
                      onChange={(e) => setRepayType(e.target.value)}
                    />
                    원리금균등상환
                  </label>
                  <label
                    style={{
                      whiteSpace: "nowrap",
                      flex: "1 1 45%",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      fontSize: "14px",
                      fontWeight: "600",
                      color: "#333",
                    }}
                  >
                    <input
                      type="radio"
                      name="repayType"
                      value="principal_only"
                      checked={repayType === "principal_only"}
                      onChange={(e) => setRepayType(e.target.value)}
                    />
                    원금균등상환
                  </label>
                </div>

                <button onClick={calculateLoan} style={styles.filterButton}>
                  월 상환액 계산하기
                </button>

                {monthlyPayment !== null && (
                  <div
                    style={{
                      marginTop: "12px",
                      padding: "12px",
                      background: "#f9f9f9",
                      border: "1px solid #ddd",
                      borderRadius: "8px",
                      textAlign: "left",
                      lineHeight: "1.8",
                      fontSize: "14px",
                    }}
                  >
                    {[
                      ["월 상환액: ", monthlyPayment],
                      ["총 상환금액: ", totalPayment],
                      ["총 이자액: ", totalInterest],
                    ].map(([label, value]) => (
                      <div key={label} style={styles.loanResultRow}>
                        <strong style={styles.loanResultLabel}>{label}</strong>
                        <span style={styles.loanResultValue}>
                          {value.toLocaleString("ko-KR")}원
                        </span>
                      </div>
                    ))}

                    {/* 그래프 */}
                    <div
                      style={{
                        width: "100%",
                        height: "200px",
                        marginTop: "20px",
                      }}
                    >
                      <ResponsiveContainer>
                        <BarChart
                          data={[
                            {
                              name: "원금",
                              금액: totalPayment - totalInterest,
                            },
                            { name: "이자", 금액: totalInterest },
                          ]}
                          margin={{ top: 10, right: 20, left: 0, bottom: 5 }}
                        >
                          <XAxis dataKey="name" />
                          <YAxis
                            domain={[
                              0,
                              (dataMax) =>
                                Math.ceil(dataMax / 10000000) * 10000000 * 1.2,
                            ]} // ✅ 20% 여유
                            tickCount={6} // 등간격 6개 눈금 (0~최대)
                            tickFormatter={(v) =>
                              v >= 100000000
                                ? `${(v / 100000000).toFixed(1)}억`
                                : `${(v / 10000).toFixed(0)}만`
                            }
                            tick={{ fontSize: 12 }}
                            width={55}
                          />
                          <Tooltip
                            formatter={(value) =>
                              `${value.toLocaleString("ko-KR")}원`
                            }
                          />
                          <Legend />
                          <Bar
                            dataKey="금액"
                            fill="#3b82f6"
                            radius={[6, 6, 0, 0]}
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* 오른쪽 금융상품 카드 영역 */}
        <div style={styles.cardContainer}>
          <div style={styles.card}>
            <h2 style={styles.title}>금융상품</h2>

            {/* 카테고리 탭 + 정렬 */}
            <div style={styles.tabRow}>
              <div style={styles.tabs}>
                {[
                  { key: "DEPOSIT", label: "예금" },
                  { key: "SAVING", label: "적금" },
                  { key: "LOAN", label: "대출" },
                ].map((t) => (
                  <button
                    key={t.key}
                    onClick={() => setCategory(t.key)}
                    style={{
                      ...styles.tab,
                      ...(category === t.key ? styles.activeTab : {}),
                    }}
                  >
                    {t.label}
                  </button>
                ))}
              </div>

              {/* 정렬 드롭다운 */}
              <select
                value={sortOption}
                onChange={(e) => setSortOption(e.target.value)}
                style={styles.sortSelect}
              >
                <option value="id,desc">최신 등록순</option>
                <option value="productName,asc">가나다순</option>
                <option value="interestRate,desc">금리 높은순</option>{" "}
                <option value="interestRate,asc">금리 낮은순</option>{" "}
              </select>
            </div>

            {/* 검색창 */}
            <div style={styles.searchBox}>
              <input
                type="text"
                placeholder="상품명 검색"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    fetchProducts(1);
                  }
                }}
                style={styles.input}
              />
              <button onClick={() => fetchProducts(1)} style={styles.searchBtn}>
                검색
              </button>
            </div>

            {isLoanCategory && (
              <div style={styles.loanSubTabs}>
                {LOAN_TYPE_CONFIG.map(({ type, title }) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => handleLoanSubTabClick(type)}
                    style={{
                      ...styles.loanSubTab,
                      ...(activeLoanType === type
                        ? styles.loanSubTabActive
                        : {}),
                    }}
                  >
                    {title}
                  </button>
                ))}
              </div>
            )}

            {/* 결과 수 */}
            <p style={styles.resultCount}>
              총 {isLoanCategory ? displayedLoanCount : totalProductCount}개
            </p>

            {/* 리스트 */}
            {loading ? (
              <p style={{ textAlign: "center", color: "#777" }}>
                불러오는 중...
              </p>
            ) : isLoanCategory ? (
              <div style={styles.loanWrapper}>
                {visibleLoanConfigs.map(
                  ({ type, title, groups, totalGroups, totalPages, page }) => {
                    return (
                      <section key={type} style={styles.loanSection}>
                        <div style={styles.loanSectionHeader}>
                          <h3 style={styles.loanSectionTitle}>{title}</h3>
                        </div>
                        {totalGroups === 0 ? (
                          <p style={styles.loanEmpty}>
                            등록된 상품이 없습니다.
                          </p>
                        ) : (
                          <>
                            <div style={styles.loanList}>
                              {groups.map((group, groupIdx) => (
                                <div
                                  key={`${type}-${group.productName}-${group.companyName}-${groupIdx}`}
                                  style={styles.loanItem}
                                  onMouseEnter={(e) =>
                                    (e.currentTarget.style.boxShadow =
                                      "0 6px 16px rgba(0,0,0,0.1)")
                                  }
                                  onMouseLeave={(e) =>
                                    (e.currentTarget.style.boxShadow =
                                      "0 2px 8px rgba(0,0,0,0.04)")
                                  }
                                >
                                  <div style={styles.loanItemHeader}>
                                    {/* 제목 + 별 */}
                                    <div
                                      style={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "6px",
                                      }}
                                    >
                                      <div
                                        style={{
                                          transform: "translateY(-1px)",
                                        }}
                                      >
                                        <FavoriteStar
                                          productId={group.productId}
                                        />
                                      </div>

                                      <div>
                                        <h4 style={styles.loanItemTitle}>
                                          {group.productName}
                                        </h4>
                                        <span style={styles.loanProvider}>
                                          {group.companyName}
                                        </span>
                                      </div>
                                    </div>

                                    {/* 오른쪽 대출타입 뱃지 */}
                                    <span style={styles.loanBadge}>
                                      {title}
                                    </span>
                                  </div>

                                  <div style={styles.loanVariantList}>
                                    {group.variants.map(
                                      (variant, variantIdx) => (
                                        <div
                                          key={`${type}-${groupIdx}-variant-${variantIdx}`}
                                          style={
                                            group.variants.length > 1
                                              ? styles.loanVariant
                                              : styles.loanVariantSingle
                                          }
                                        >
                                          {group.variants.length > 1 && (
                                            <div
                                              style={styles.loanVariantHeader}
                                            >
                                              <span
                                                style={styles.loanVariantBadge}
                                              >
                                                옵션 {variantIdx + 1}
                                              </span>
                                              {(variant.lendTypeName ||
                                                variant.rpayTypeName ||
                                                variant.crdtLendRateTypeNm) && (
                                                <span
                                                  style={
                                                    styles.loanVariantSummary
                                                  }
                                                >
                                                  {variant.lendTypeName ||
                                                    variant.rpayTypeName ||
                                                    variant.crdtLendRateTypeNm}
                                                </span>
                                              )}
                                            </div>
                                          )}
                                          <div style={styles.loanFields}>
                                            {buildLoanFields(type, variant).map(
                                              (field, fieldIdx) => (
                                                <div
                                                  key={`${type}-${groupIdx}-variant-${variantIdx}-${field.label}-${fieldIdx}`}
                                                  style={styles.loanFieldRow}
                                                >
                                                  <span
                                                    style={
                                                      styles.loanFieldLabel
                                                    }
                                                  >
                                                    {field.label}
                                                  </span>
                                                  <span
                                                    style={
                                                      styles.loanFieldValue
                                                    }
                                                  >
                                                    {field.value}
                                                  </span>
                                                </div>
                                              )
                                            )}
                                          </div>
                                        </div>
                                      )
                                    )}
                                  </div>
                                </div>
                              ))}
                            </div>
                            {totalPages > 1 && (
                              <div style={styles.loanPagination}>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleLoanPageChange(
                                      type,
                                      page - 1,
                                      totalPages
                                    )
                                  }
                                  disabled={page === 1}
                                  style={{
                                    ...styles.loanPaginationButton,
                                    ...(page === 1
                                      ? styles.loanPaginationButtonDisabled
                                      : {}),
                                  }}
                                >
                                  이전
                                </button>
                                <div style={styles.loanPaginationPages}>
                                  {buildPageNumbers(page, totalPages).map(
                                    (pageNumber) => (
                                      <button
                                        key={pageNumber}
                                        type="button"
                                        onClick={() =>
                                          handleLoanPageChange(
                                            type,
                                            pageNumber,
                                            totalPages
                                          )
                                        }
                                        style={{
                                          ...styles.loanPaginationPage,
                                          ...(pageNumber === page
                                            ? styles.loanPaginationPageActive
                                            : {}),
                                        }}
                                      >
                                        {pageNumber}
                                      </button>
                                    )
                                  )}
                                </div>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleLoanPageChange(
                                      type,
                                      page + 1,
                                      totalPages
                                    )
                                  }
                                  disabled={page === totalPages}
                                  style={{
                                    ...styles.loanPaginationButton,
                                    ...(page === totalPages
                                      ? styles.loanPaginationButtonDisabled
                                      : {}),
                                  }}
                                >
                                  다음
                                </button>
                              </div>
                            )}
                          </>
                        )}
                      </section>
                    );
                  }
                )}
              </div>
            ) : (
              <div>
                <div style={styles.list}>
                  {products.length === 0 ? (
                    <p style={{ textAlign: "center", color: "#888" }}>
                      해당 조건의 상품이 없습니다.
                    </p>
                  ) : (
                    products.map((item) => (
                      <div
                        key={item.id}
                        style={styles.item}
                        onMouseEnter={(e) =>
                          (e.currentTarget.style.boxShadow =
                            "0 6px 16px rgba(0,0,0,0.1)")
                        }
                        onMouseLeave={(e) =>
                          (e.currentTarget.style.boxShadow =
                            "0 2px 8px rgba(0,0,0,0.04)")
                        }
                      >
                        <div style={styles.itemHeader}>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "6px",
                            }}
                          >
                            <div style={{ transform: "translateY(-1px)" }}>
                              <FavoriteStar productId={item.productId} />
                            </div>

                            <h3 style={styles.itemTitle}>{item.productName}</h3>
                          </div>

                          <span style={styles.provider}>{item.provider}</span>
                        </div>
                        <pre
                          style={styles.condition}
                          dangerouslySetInnerHTML={{
                            __html: (item.joinCondition || "가입 조건 없음")
                              .replace(/:/g, "")
                              .replace(/\*/g, "")
                              .replace(/(가입 방법)/g, "<strong>$1</strong>")
                              .replace(/(가입 대상)/g, "<strong>$1</strong>")
                              .replace(/(비고)/g, "<strong>$1</strong>"),
                          }}
                        />
                        <div style={styles.infoRow}>
                          <span>금리: {item.interestRate ?? "-"}%</span>
                        </div>
                        {item.detailUrl && (
                          <a
                            href={item.detailUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={styles.link}
                          >
                            자세히 보기 →
                          </a>
                        )}
                      </div>
                    ))
                  )}
                </div>
                {totalProductPages > 1 && (
                  <div style={styles.loanPagination}>
                    <button
                      type="button"
                      onClick={() => handleProductPageChange(productPage - 1)}
                      disabled={productPage === 1}
                      style={{
                        ...styles.loanPaginationButton,
                        ...(productPage === 1
                          ? styles.loanPaginationButtonDisabled
                          : {}),
                      }}
                    >
                      이전
                    </button>
                    <div style={styles.loanPaginationPages}>
                      {buildPageNumbers(productPage, totalProductPages).map(
                        (pageNumber) => (
                          <button
                            key={pageNumber}
                            type="button"
                            onClick={() => handleProductPageChange(pageNumber)}
                            style={{
                              ...styles.loanPaginationPage,
                              ...(pageNumber === productPage
                                ? styles.loanPaginationPageActive
                                : {}),
                            }}
                          >
                            {pageNumber}
                          </button>
                        )
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => handleProductPageChange(productPage + 1)}
                      disabled={productPage === totalProductPages}
                      style={{
                        ...styles.loanPaginationButton,
                        ...(productPage === totalProductPages
                          ? styles.loanPaginationButtonDisabled
                          : {}),
                      }}
                    >
                      다음
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  );
}

const styles = {
  page: {
    display: "grid",
    gridTemplateColumns: "250px 1.3fr",
    gap: "40px",
    maxWidth: "1200px",
    margin: "0",
    padding: "80px 20px",
    background: "#fdfaf6",
    alignItems: "start",
    transform: "translateX(-185px)",
  },
  sidebar: {
    background: "#fff",
    borderRadius: "12px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "25px 20px",
    height: "fit-content",
    position: "static",
    top: "100px",
  },
  filterTitle: {
    fontSize: "18px",
    fontWeight: "700",
    marginBottom: "20px",
  },
  filterGroup: {
    marginBottom: "25px",
  },
  filterLabel: {
    display: "block",
    fontSize: "14px",
    fontWeight: "600",
    marginBottom: "8px",
  },
  dropdownButton: {
    width: "100%",
    border: "0px solid #ddd",
    borderRadius: "8px",
    padding: "3px",
    fontSize: "14px",
    fontWeight: "500",
    textAlign: "left",
    cursor: "pointer",
    background: "#fff",
  },
  dropdownList: {
    marginTop: "6px",
    borderRadius: "8px",
    background: "#fff",
    maxHeight: "180px",
    overflowY: "auto",
    padding: "8px",
  },
  checkboxLabel: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    padding: "4px 0",
    fontSize: "14px",
    cursor: "pointer",
  },

  rateText: {
    fontSize: "13px",
    color: "#555",
    marginTop: "8px",
  },
  filterButton: {
    width: "100%",
    background: "#eeeeeeff",
    border: "1px solid #fff",
    color: "#333",
    borderRadius: "8px",
    padding: "10px 0",
    fontWeight: "600",
    cursor: "pointer",
    transition: "all 0.2s ease",
    marginTop: "12px",
  },
  resetButton: {
    width: "100%",
    background: "#eeeeeeff",
    border: "1px solid #fff",
    color: "#333",
    borderRadius: "8px",
    padding: "10px 0",
    fontWeight: "600",
    cursor: "pointer",
    marginTop: "12px",
    transition: "all 0.2s ease",
  },
  cardContainer: {
    flex: 1,
  },
  card: {
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 35px",
    width: "100%",
    minHeight: "700px",
  },
  title: {
    fontSize: "22px",
    fontWeight: "700",
    marginBottom: "25px",
    textAlign: "center",
  },
  tabRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "20px",
  },
  tabs: {
    display: "flex",
    gap: "12px",
  },
  tab: {
    background: "#f5f5f5",
    border: "none",
    borderRadius: "8px",
    padding: "8px 16px",
    cursor: "pointer",
    color: "#555",
    fontWeight: "500",
  },
  activeTab: {
    background: "#9ed8b5",
    color: "#fff",
    fontWeight: "600",
  },
  sortSelect: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "8px 12px",
    fontSize: "14px",
    background: "#fff",
    cursor: "pointer",
  },
  searchBox: {
    display: "flex",
    gap: "10px",
    marginBottom: "20px",
  },
  input: {
    flex: 1,
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px",
  },
  searchBtn: {
    background: "#9ed8b5",
    border: "none",
    color: "#fff",
    borderRadius: "8px",
    padding: "10px 16px",
    cursor: "pointer",
    fontWeight: "600",
  },
  resultCount: {
    color: "#555",
    marginBottom: "15px",
  },
  loanSubTabs: {
    display: "flex",
    gap: "10px",
    marginBottom: "18px",
    flexWrap: "wrap",
  },
  loanSubTab: {
    flex: "0 1 auto",
    padding: "8px 14px",
    borderRadius: "8px",
    border: "none",
    background: "#f5f5f5",
    color: "#555",
    fontSize: "14px",
    fontWeight: "500",
    cursor: "pointer",
  },
  loanSubTabActive: {
    background: "#9ed8b5",
    color: "#fff",
    fontWeight: "600",
  },
  loanWrapper: {
    display: "flex",
    flexDirection: "column",
    gap: "24px",
  },
  loanSection: {
    background: "#fff",
    borderRadius: "12px",
    border: "1px solid #eee",
    padding: "22px 24px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
  },
  loanSectionHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "12px",
  },
  loanSectionTitle: {
    fontSize: "18px",
    fontWeight: "600",
  },
  loanEmpty: {
    fontSize: "14px",
    color: "#777",
    textAlign: "center",
    padding: "12px 0",
  },
  loanList: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  loanPagination: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "10px",
    marginTop: "18px",
    flexWrap: "wrap",
  },
  loanPaginationButton: {
    padding: "6px 12px",
    borderRadius: "6px",
    border: "1px solid #ddd",
    background: "#fff",
    color: "#555",
    fontSize: "13px",
    fontWeight: "500",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  loanPaginationButtonDisabled: {
    color: "#bbb",
    border: "1px solid #ddd",
    background: "#f9f9f9",
    cursor: "not-allowed",
  },
  loanPaginationPages: {
    display: "flex",
    gap: "6px",
    alignItems: "center",
  },
  loanPaginationPage: {
    padding: "6px 10px",
    borderRadius: "6px",
    border: "1px solid #ddd",
    background: "#fff",
    color: "#555",
    fontSize: "13px",
    fontWeight: "500",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  loanPaginationPageActive: {
    background: "#9ed8b5",
    border: "1px solid #ddd",
    color: "#fff",
  },
  loanItem: {
    border: "1px solid #f0f0f0",
    borderRadius: "10px",
    padding: "18px 20px",
    background: "#fafafa",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    transition: "box-shadow 0.2s ease",
  },
  loanItemHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    marginBottom: "12px",
  },
  loanItemTitle: {
    fontSize: "16px",
    fontWeight: "600",
    marginBottom: "4px",
  },
  loanProvider: {
    fontSize: "13px",
    color: "#666",
  },
  loanBadge: {
    fontSize: "12px",
    color: "#4a8f66",
    background: "#e4f3eb",
    borderRadius: "999px",
    padding: "4px 10px",
    fontWeight: "600",
  },
  loanFields: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
    gap: "10px",
  },
  loanVariantList: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },
  loanVariant: {
    background: "#fff",
    border: "1px solid #e8e8e8",
    borderRadius: "10px",
    padding: "12px 14px",
    boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
  },
  loanVariantSingle: {
    background: "transparent",
    padding: "0",
    border: "none",
    boxShadow: "none",
  },
  loanVariantHeader: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: "10px",
  },
  loanVariantBadge: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#eef8f1",
    color: "#4a8f66",
    borderRadius: "999px",
    padding: "4px 10px",
    fontSize: "12px",
    fontWeight: "600",
  },
  loanVariantSummary: {
    fontSize: "13px",
    color: "#666",
    fontWeight: "500",
  },
  loanFieldRow: {
    display: "flex",
    flexDirection: "column",
    background: "#fff",
    borderRadius: "8px",
    border: "1px solid #f2f2f2",
    padding: "10px 12px",
  },
  loanFieldLabel: {
    fontSize: "12px",
    color: "#888",
    marginBottom: "4px",
  },
  loanFieldValue: {
    fontSize: "14px",
    fontWeight: "600",
    color: "#333",
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  item: {
    background: "#fff",
    border: "1px solid #eee",
    borderRadius: "12px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    padding: "22px 26px",
    transition: "box-shadow 0.2s ease",
  },
  itemHeader: {
    display: "flex",
    justifyContent: "space-between",
    marginBottom: "8px",
  },
  itemTitle: {
    fontSize: "17px",
    fontWeight: "600",
  },
  provider: {
    fontSize: "14px",
    color: "#777",
  },
  condition: {
    fontSize: "13px",
    color: "#555",
    marginBottom: "10px",
    whiteSpace: "pre-line",
    lineHeight: "1.7",
    letterSpacing: "0.2px",
    fontFamily: "Pretendard, 'Noto Sans KR', sans-serif",
    background: "#fafafa",
    borderRadius: "8px",
    padding: "10px 12px",
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  infoRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "14px",
    color: "#444",
  },
  link: {
    marginTop: "8px",
    display: "inline-block",
    color: "#0077cc",
    textDecoration: "none",
    fontSize: "13px",
    fontWeight: "500",
  },
  checkboxGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
    padding: "8px 4px",
    borderRadius: "8px",
    background: "#fff",
  },
  sidebarContainer: {
    display: "flex",
    flexDirection: "column",
    gap: "25px",
    paddingRight: "6px",
  },
  inputBox: {
    width: "100%",
    height: "32px",
    border: "1px solid #ddd",
    borderRadius: "6px",
    padding: "6px 10px",
    fontSize: "14px",
    color: "#333",
    outline: "none",
    boxSizing: "border-box",
    marginTop: "6px",
    transition: "border-color 0.2s ease, box-shadow 0.2s ease",
    fontFamily: "inherit",
  },
  loanResultRow: {
    display: "flex",
    alignItems: "center",
    gap: "5px",
    marginBottom: "4px",
    textAlign: "left",
  },
  loanResultLabel: {
    fontSize: "14px",
    fontWeight: "600",
    color: "#333",
  },
  loanResultValue: {
    fontSize: "14px",
    color: "#444",
  },
};
