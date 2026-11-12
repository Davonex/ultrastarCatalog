/*
 * Catalog page:
 * - navigation without reloading the page: search, filters, sort and view replace only #app,
 *   so the song being played (and the mini player) are not interrupted; the URL is updated;
 * - infinite scroll: the next pages of songs are loaded while scrolling (/?...&page=N&fragment=1);
 * - favorites (stored in this browser) and the song information popup.
 */
(function () {

    // ---------- Favorites (localStorage) ----------
    function favorites() {
        try {
            return JSON.parse(localStorage.getItem('favorites')) || [];
        } catch (e) {
            return [];
        }
    }

    window.toggleFavorite = function (event, icon) {
        event.stopPropagation();
        var songId = icon.closest('.flip-card-inner').getAttribute('data-songid');
        var list = favorites();
        var index = list.indexOf(songId);
        if (index === -1) {
            list.push(songId);
        } else {
            list.splice(index, 1);
        }
        icon.classList.toggle('fa-solid', index === -1);
        icon.classList.toggle('fa-regular', index !== -1);
        localStorage.setItem('favorites', JSON.stringify(list));
    };

    function markFavorites(root) {
        var list = favorites();
        root.querySelectorAll('.flip-card-inner').forEach(function (card) {
            var icon = card.querySelector('.favorite-icon');
            var isFavorite = list.indexOf(card.getAttribute('data-songid')) !== -1;
            if (icon) {
                icon.classList.toggle('fa-solid', isFavorite);
                icon.classList.toggle('fa-regular', !isFavorite);
            }
        });
    }

    // ---------- Song information popup ----------
    window.openSongInfo = function (event, btn) {
        event.stopPropagation();
        event.preventDefault();
        fetch('/songinfo/' + encodeURIComponent(btn.getAttribute('data-songid')))
            .then(function (r) {
                if (!r.ok) {
                    throw new Error(r.status);
                }
                return r.text();
            })
            .then(function (html) {
                document.getElementById('modalContent').innerHTML = html;
                document.getElementById('modalLayer').style.display = 'block';
            })
            .catch(function () { /* song not found: nothing to show */ });
    };

    window.closeModal = function () {
        document.getElementById('modalLayer').style.display = 'none';
    };

    document.addEventListener('click', function (e) {
        if (e.target === document.getElementById('modalLayer')) {
            window.closeModal();
        }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            window.closeModal();
        }
    });

    // ---------- Navigation without reloading ----------
    // URL described by the form (empty search left out, favorites ids added when "Favorites" is on)
    function formUrl() {
        var form = document.getElementById('catalogForm');
        var favs = document.getElementById('f-favs');
        if (favs && !favs.disabled) {
            favs.value = favorites().join(',');
        }
        var search = document.getElementById('search');
        var emptySearch = search && !search.value.trim();
        if (emptySearch) {
            search.disabled = true;
        }
        var query = new URLSearchParams(new FormData(form)).toString();
        if (emptySearch) {
            search.disabled = false;
        }
        return '/' + (query ? '?' + query : '');
    }

    var navigationId = 0;

    /**
     * Loads a catalog URL and replaces #app with the new content.
     * history: 'push' (new entry), 'replace' (same entry) or 'none' (back/forward button).
     */
    function navigate(url, history) {
        var id = ++navigationId;
        document.body.classList.add('loading');
        fetch(url, {headers: {'Accept': 'text/html'}})
            .then(function (r) {
                if (!r.ok) {
                    throw new Error(r.status);
                }
                return r.text();
            })
            .then(function (html) {
                if (id !== navigationId) {
                    return; // a newer navigation was started in the meantime
                }
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var newApp = doc.getElementById('app');
                if (!newApp) {
                    throw new Error('unexpected page');
                }
                document.getElementById('app').replaceWith(document.adoptNode(newApp));
                document.title = doc.title;
                if (history === 'push') {
                    window.history.pushState({catalog: true}, '', url);
                } else if (history === 'replace') {
                    window.history.replaceState({catalog: true}, '', url);
                }
                document.body.classList.remove('no-scroll');
                window.scrollTo(0, 0);
                initPage();
            })
            .catch(function () {
                window.location.href = url; // fallback: normal page load
            })
            .then(function () {
                if (id === navigationId) {
                    document.body.classList.remove('loading');
                }
            });
    }

    window.navigateCatalog = function (url) {
        navigate(url, 'push');
    };
    window.submitCatalogForm = function () {
        navigate(formUrl(), 'push');
    };

    // Search with the Enter key or the magnifier button
    document.addEventListener('submit', function (e) {
        if (e.target && e.target.id === 'catalogForm') {
            e.preventDefault();
            window.submitCatalogForm();
        }
    });

    // Back / forward buttons of the browser
    window.addEventListener('popstate', function () {
        navigate(window.location.pathname + window.location.search, 'none');
    });

    // ---------- Infinite scroll ----------
    var observer = null;
    var loadingPage = false;

    function sentinelIsNear(sentinel) {
        return sentinel.getBoundingClientRect().top < window.innerHeight + 800;
    }

    function loadNextPage() {
        var sentinel = document.getElementById('loadMore');
        if (loadingPage || !sentinel) {
            return;
        }
        loadingPage = true;
        var page = parseInt(sentinel.getAttribute('data-next-page'), 10);
        var params = new URLSearchParams(window.location.search);
        params.set('page', page);
        params.set('fragment', '1');
        var hasMore = false;
        fetch('/?' + params.toString(), {headers: {'Accept': 'text/html'}})
            .then(function (r) {
                if (!r.ok) {
                    throw new Error(r.status);
                }
                hasMore = r.headers.get('X-Has-More') === 'true';
                return r.text();
            })
            .then(function (html) {
                if (!document.body.contains(sentinel)) {
                    return; // the page was replaced in the meantime
                }
                var template = document.createElement('template');
                template.innerHTML = html;
                var target = document.getElementById('songRows') || document.getElementById('songGrid');
                Array.prototype.slice.call(template.content.children).forEach(function (node) {
                    target.appendChild(node);
                });
                markFavorites(target);
                if (window.refreshPlayerButtons) {
                    window.refreshPlayerButtons();
                }
                if (hasMore) {
                    sentinel.setAttribute('data-next-page', String(page + 1));
                } else {
                    sentinel.remove();
                }
            })
            .catch(function () { /* network error: a new scroll will try again */ })
            .then(function () {
                loadingPage = false;
                var s = document.getElementById('loadMore');
                if (s && hasMore && sentinelIsNear(s)) {
                    loadNextPage(); // short pages: keep loading until the screen is filled
                }
            });
    }

    function setupInfiniteScroll() {
        if (observer) {
            observer.disconnect();
            observer = null;
        }
        var sentinel = document.getElementById('loadMore');
        if (!sentinel) {
            return;
        }
        if (!('IntersectionObserver' in window)) {
            window.addEventListener('scroll', function () {
                var s = document.getElementById('loadMore');
                if (s && sentinelIsNear(s)) {
                    loadNextPage();
                }
            });
            return;
        }
        observer = new IntersectionObserver(function (entries) {
            if (entries.some(function (entry) { return entry.isIntersecting; })) {
                loadNextPage();
            }
        }, {rootMargin: '800px 0px'});
        observer.observe(sentinel);
    }

    // ---------- Page initialisation (first load and after each navigation) ----------
    function initPage() {
        // "Favorites" on but no ids in the URL (bookmark, old link): reload with the favorites of this browser
        var fav = document.getElementById('f-fav');
        if (fav && !fav.disabled && !new URLSearchParams(window.location.search).has('favs')) {
            navigate(formUrl(), 'replace');
            return;
        }
        markFavorites(document);
        setupInfiniteScroll();
        if (window.refreshPlayerButtons) {
            window.refreshPlayerButtons();
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        window.history.replaceState({catalog: true}, '', window.location.href);
        initPage();
    });
})();
