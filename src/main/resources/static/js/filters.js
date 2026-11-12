/*
 * Top of the catalog: filter selection screens (Language, Decade, Tags, Playlists), sort menu,
 * favorites on/off and grid/list view. All of them write into the form #catalogForm and send it
 * (through catalog.js, without reloading the page), so the URL always describes the current selection.
 */
(function () {
    var openModal = null;       // filter screen currently open
    var initialSelection = [];  // its selection when it was opened (restored on cancel)

    function form() {
        return document.getElementById('catalogForm');
    }

    // Sends the form (catalog.js loads the result without reloading the page)
    function submitForm() {
        document.body.classList.remove('no-scroll');
        openModal = null;
        if (window.submitCatalogForm) {
            window.submitCatalogForm();
        } else {
            form().submit();
        }
    }

    function selectedValues(modal) {
        return Array.prototype.map.call(modal.querySelectorAll('.filter-tile.selected'), function (tile) {
            return tile.getAttribute('data-value');
        });
    }

    window.openFilter = function (param) {
        var modal = document.getElementById('filter-' + param);
        if (!modal) {
            return;
        }
        closeSortMenu();
        openModal = modal;
        initialSelection = selectedValues(modal);
        modal.hidden = false;
        document.body.classList.add('no-scroll');
    };

    // Close without applying: the selection goes back to what it was
    window.closeFilter = function () {
        if (!openModal) {
            return;
        }
        openModal.querySelectorAll('.filter-tile').forEach(function (tile) {
            tile.classList.toggle('selected', initialSelection.indexOf(tile.getAttribute('data-value')) !== -1);
        });
        openModal.hidden = true;
        openModal = null;
        document.body.classList.remove('no-scroll');
    };

    window.toggleTile = function (tile) {
        tile.classList.toggle('selected');
    };

    window.clearFilterSelection = function () {
        if (openModal) {
            openModal.querySelectorAll('.filter-tile.selected').forEach(function (tile) {
                tile.classList.remove('selected');
            });
        }
    };

    // Writes the chosen values into the hidden fields of the form, then loads the catalog
    window.applyFilter = function () {
        if (!openModal) {
            return;
        }
        var param = openModal.getAttribute('data-param');
        var container = document.getElementById('hidden-' + param);
        container.innerHTML = '';
        selectedValues(openModal).forEach(function (value) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = param;
            input.value = value;
            container.appendChild(input);
        });
        openModal.hidden = true;
        submitForm();
    };

    // ---- Sort menu ----
    function closeSortMenu() {
        var menu = document.getElementById('sortMenu');
        if (menu) {
            menu.hidden = true;
        }
    }

    window.toggleSortMenu = function (event) {
        event.stopPropagation();
        var menu = document.getElementById('sortMenu');
        menu.hidden = !menu.hidden;
    };

    // Sort field (artist, title, language, year, added): the order (asc/desc) is kept,
    // except for "added" (most recent first)
    window.chooseSort = function (sort) {
        var sortField = document.getElementById('f-sort');
        if (sortField.value === sort) {
            closeSortMenu();
            return;
        }
        sortField.value = sort;
        if (sort === 'added') {
            document.getElementById('f-dir').value = 'desc';
        }
        submitForm();
    };

    // Order button: ascending <-> descending
    window.toggleSortOrder = function () {
        var dirField = document.getElementById('f-dir');
        dirField.value = dirField.value === 'desc' ? 'asc' : 'desc';
        submitForm();
    };

    // ---- Favorites, view, clear ----
    window.toggleFavoritesFilter = function () {
        var fav = document.getElementById('f-fav');
        fav.disabled = !fav.disabled;
        document.getElementById('f-favs').disabled = fav.disabled;
        submitForm();
    };

    window.toggleView = function () {
        var view = document.getElementById('f-view');
        view.disabled = !view.disabled;
        submitForm();
    };

    // Removes search and filters, keeps the sort and the view
    window.clearAllFilters = function (event) {
        event.preventDefault();
        var url = '/?sort=' + encodeURIComponent(document.getElementById('f-sort').value)
            + '&dir=' + encodeURIComponent(document.getElementById('f-dir').value);
        if (!document.getElementById('f-view').disabled) {
            url += '&view=list';
        }
        if (window.navigateCatalog) {
            window.navigateCatalog(url, true);
        } else {
            window.location.href = url;
        }
    };

    // Click outside: close the sort menu; click on the dark background: close the filter screen
    document.addEventListener('click', function (e) {
        var menu = document.getElementById('sortMenu');
        if (menu && !menu.hidden && !menu.contains(e.target)) {
            closeSortMenu();
        }
        if (openModal && e.target === openModal) {
            window.closeFilter();
        }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeSortMenu();
            window.closeFilter();
        }
    });
})();
