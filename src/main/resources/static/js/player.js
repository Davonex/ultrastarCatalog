/*
 * Audio preview played from the song cards and from the list view, with a mini player at the bottom
 * of the screen (#nowPlaying): cover, artist : title, play/pause, progress bar (seek) and stop.
 * One shared Audio object: starting a song stops the previous one. The page content can be replaced
 * (filters, sort...) without interrupting the song: the mini player is outside #app.
 * Buttons: <button data-songid data-start data-artist data-title onclick="togglePlay(event, this)">
 */
(function () {
    var player = new Audio();
    player.preload = 'none';
    var current = null; // {id, start, artist, title}
    var seeking = false;

    function labels() {
        return window.playerLabels || {play: 'Play', pause: 'Pause'};
    }

    function isPlaying() {
        return current !== null && !player.paused && !player.ended;
    }

    // ---------- Play buttons of the cards / list ----------
    function refreshButtons() {
        var playing = isPlaying();
        document.querySelectorAll('.play-button, .play-button-small').forEach(function (btn) {
            var isCurrent = playing && btn.getAttribute('data-songid') === current.id;
            if (btn.classList.contains('playing') === isCurrent && btn.dataset.rendered) {
                return; // nothing changed for this button
            }
            btn.dataset.rendered = 'true';
            btn.classList.toggle('playing', isCurrent);
            var label = isCurrent ? labels().pause : labels().play;
            btn.title = label;
            btn.setAttribute('aria-label', label);
            // A new <i> each time, so it also works when Font Awesome replaces icons by SVG
            btn.innerHTML = '<i class="fa-solid ' + (isCurrent ? 'fa-pause' : 'fa-play') + '"></i>';
        });
        refreshMiniPlayer();
    }
    window.refreshPlayerButtons = refreshButtons;

    // ---------- Mini player ----------
    function bar() {
        return document.getElementById('nowPlaying');
    }

    function formatTime(seconds) {
        if (!isFinite(seconds) || seconds < 0) {
            return '0:00';
        }
        var s = Math.floor(seconds);
        return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
    }

    function refreshMiniPlayer() {
        var np = bar();
        if (!np) {
            return;
        }
        np.hidden = current === null;
        document.body.classList.toggle('has-player', current !== null);
        if (current === null) {
            return;
        }
        np.querySelector('.np-artist').textContent = current.artist || '';
        np.querySelector('.np-song').textContent = current.title || '';
        var cover = np.querySelector('.np-cover');
        var src = '/images/' + encodeURIComponent(current.id) + '.jpg?w=120';
        if (cover.getAttribute('src') !== src) {
            cover.setAttribute('src', src);
        }
        var toggle = np.querySelector('.np-toggle');
        var playing = isPlaying();
        var label = playing ? labels().pause : labels().play;
        if (toggle.dataset.state !== String(playing)) {
            toggle.dataset.state = String(playing);
            toggle.title = label;
            toggle.setAttribute('aria-label', label);
            toggle.innerHTML = '<i class="fa-solid ' + (playing ? 'fa-pause' : 'fa-play') + '"></i>';
        }
        refreshProgress();
    }

    function refreshProgress() {
        var np = bar();
        if (!np || current === null || seeking) {
            return;
        }
        var duration = player.duration;
        np.querySelector('.np-time').textContent = formatTime(player.currentTime);
        np.querySelector('.np-duration').textContent = formatTime(duration);
        np.querySelector('.np-seek').value = isFinite(duration) && duration > 0
            ? Math.round(player.currentTime / duration * 1000) : 0;
    }

    function stop() {
        player.pause();
        player.removeAttribute('src');
        player.load();
        current = null;
        refreshButtons();
    }

    // ---------- Playback ----------
    function start(song) {
        current = song;
        player.pause();
        player.src = '/audio/' + encodeURIComponent(song.id) + '.mp3';
        if (song.start > 0) {
            player.addEventListener('loadedmetadata', function () {
                if (isNaN(player.duration) || song.start < player.duration) {
                    player.currentTime = song.start;
                }
            }, {once: true});
        }
        refreshButtons();
        var promise = player.play(); // called inside the click: allowed on iOS / Android
        if (promise && promise.catch) {
            promise.catch(function (e) {
                if (e && e.name !== 'AbortError') {
                    console.warn('Audio preview failed', e);
                }
                refreshButtons();
            });
        }
    }

    function resumeOrPause() {
        if (current === null) {
            return;
        }
        if (player.paused || player.ended) {
            if (player.ended) {
                player.currentTime = current.start > 0 ? current.start : 0;
            }
            player.play();
        } else {
            player.pause();
        }
    }

    window.togglePlay = function (event, btn) {
        event.stopPropagation(); // do not open the song popup
        event.preventDefault();
        var id = btn.getAttribute('data-songid');
        if (current !== null && id === current.id && player.src && !player.ended) {
            resumeOrPause();
            return;
        }
        start({
            id: id,
            start: parseFloat(btn.getAttribute('data-start')) || 0,
            artist: btn.getAttribute('data-artist') || '',
            title: btn.getAttribute('data-title') || ''
        });
    };

    ['play', 'playing', 'pause', 'ended', 'error', 'emptied', 'loadedmetadata'].forEach(function (name) {
        player.addEventListener(name, refreshButtons);
    });
    player.addEventListener('timeupdate', refreshProgress);

    // Mini player controls
    document.addEventListener('DOMContentLoaded', function () {
        var np = bar();
        if (!np) {
            return;
        }
        np.querySelector('.np-toggle').addEventListener('click', resumeOrPause);
        np.querySelector('.np-close').addEventListener('click', stop);
        var seek = np.querySelector('.np-seek');
        seek.addEventListener('input', function () {
            seeking = true;
            if (isFinite(player.duration)) {
                np.querySelector('.np-time').textContent = formatTime(seek.value / 1000 * player.duration);
            }
        });
        seek.addEventListener('change', function () {
            if (isFinite(player.duration) && player.duration > 0) {
                player.currentTime = seek.value / 1000 * player.duration;
            }
            seeking = false;
            refreshProgress();
        });
        np.querySelector('.np-cover').addEventListener('error', function () {
            this.src = '/img/no-cover.svg';
        });
    });
})();
