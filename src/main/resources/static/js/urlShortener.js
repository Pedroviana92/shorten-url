async function shortenUrl() {
    const urlInput = document.getElementById('urlInput');
    const submitBtn = document.getElementById('submitBtn');
    const result = document.getElementById('result');
    const error = document.getElementById('error');
    const shortenedLink = document.getElementById('shortenedLink');

    const url = urlInput.value.trim();

    if (!url) {
        error.textContent = 'Please enter a URL';
        result.style.display = 'none';
        return;
    }

    // Disable button and show loading state
    submitBtn.disabled = true;
    const originalText = submitBtn.textContent;
    submitBtn.textContent = 'Shortening...';

    try {
        const response = await fetch('/shorten', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ url: url })
        });

        if (!response.ok) {
            const errorData = await response.json();
            const errorMessage = errorData.message || errorData.error || 'Failed to shorten URL';
            throw new Error(errorMessage);
        }

        const data = await response.json();

        shortenedLink.href = data.shortUrl;
        shortenedLink.textContent = data.shortUrl;
        result.style.display = 'block';
        error.textContent = '';

    } catch (err) {
        error.textContent = err.message;
        result.style.display = 'none';
    } finally {
        // Re-enable button and restore text
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
    }
}

// Allow Enter key to submit
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('urlInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            shortenUrl();
        }
    });
});
