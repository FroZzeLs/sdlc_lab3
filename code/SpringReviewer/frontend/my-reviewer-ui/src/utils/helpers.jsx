export const getRatingColor = (rating) => {
    if (rating === null || rating === undefined) {
        return 'default';
    }
    const numericRating = Number(rating);
    if (isNaN(numericRating)) {
        return 'default';
    }

    if (numericRating >= 0 && numericRating < 4) {
        return 'error';
    } else if (numericRating >= 4 && numericRating < 7) {
        return 'warning';
    } else if (numericRating >= 7 && numericRating < 9) {
        return 'gold';
    } else if (numericRating >= 9 && numericRating <= 10) {
        return 'success';
    } else {
        return 'default';
    }
};