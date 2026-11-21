const messageInput = document.querySelector("#message");
const charCount = document.querySelector("#charCount");
const analyzeBtn = document.querySelector("#analyzeBtn");
const clearBtn = document.querySelector("#clearBtn");
const resultCard = document.querySelector("#result");

messageInput.addEventListener("input", () => {
    charCount.textContent = `${messageInput.value.length} / 5000`;
});

clearBtn.addEventListener("click", () => {
    messageInput.value = "";
    charCount.textContent = "0 / 5000";
    resultCard.hidden = true;
    messageInput.focus();
});

document.querySelectorAll(".sample").forEach(button => {
    button.addEventListener("click", () => {
        messageInput.value = button.dataset.message;
        charCount.textContent = `${messageInput.value.length} / 5000`;
        analyzeBtn.click();
    });
});

analyzeBtn.addEventListener("click", async () => {
    const message = messageInput.value.trim();
    if (!message) {
        messageInput.focus();
        return;
    }

    analyzeBtn.disabled = true;
    analyzeBtn.querySelector("span").textContent = "Analyzing...";
    try {
        const response = await fetch("/api/detect", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message })
        });
        if (!response.ok) throw new Error("The message could not be analyzed.");
        const data = await response.json();
        renderResult(data);
    } catch (error) {
        alert(error.message);
    } finally {
        analyzeBtn.disabled = false;
        analyzeBtn.querySelector("span").textContent = "Analyze message";
    }
});

function renderResult(data) {
    const classification = data.classification.toLowerCase();
    const icon = document.querySelector("#classificationIcon");
    const resultHeading = document.querySelector("#classification");
    const score = Math.round(data.riskScore);

    resultCard.hidden = false;
    resultHeading.textContent = data.classification.charAt(0) + data.classification.slice(1).toLowerCase();
    icon.className = `classification-icon ${classification}`;
    icon.textContent = classification === "safe" ? "✓" : classification === "suspicious" ? "!" : "⚠";
    document.querySelector("#riskScore").textContent = `${score}%`;
    document.querySelector("#explanation").textContent = data.explanation;

    const bar = document.querySelector("#scoreBar");
    bar.style.width = `${score}%`;
    bar.style.background = classification === "safe" ? "#22c55e"
        : classification === "suspicious" ? "#f59e0b" : "#e11d48";

    document.querySelector("#indicators").replaceChildren(
        ...data.detectedIndicators.map(text => {
            const li = document.createElement("li");
            li.textContent = text;
            return li;
        })
    );
    const links = document.querySelector("#links");
    if (data.suspiciousLinks.length === 0) {
        links.replaceChildren(createListItem("No links detected"));
    } else {
        links.replaceChildren(...data.suspiciousLinks.map(createListItem));
    }
    resultCard.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function createListItem(text) {
    const li = document.createElement("li");
    li.textContent = text;
    return li;
}
