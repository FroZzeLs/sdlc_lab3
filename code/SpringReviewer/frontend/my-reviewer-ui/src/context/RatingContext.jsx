import React, { createContext, useState, useEffect, useCallback, useContext, useMemo } from 'react';
import * as api from '../api';

const RatingContext = createContext({
    ratings: {},
    isLoading: true,
    error: null,
    fetchRatings: async () => {},
});

export const useRatings = () => useContext(RatingContext);

export const RatingProvider = ({ children }) => {
    const [ratings, setRatings] = useState({});
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const calculateAverageRatings = (reviews) => {
        if (!reviews || reviews.length === 0) {
            return {};
        }

        const ratingsMap = {};
        const reviewCounts = {};
        const gradeSums = {};

        reviews.forEach(review => {
            const teacherId = review.teacher?.id;
            if (teacherId && typeof review.grade === 'number') {
                gradeSums[teacherId] = (gradeSums[teacherId] || 0) + review.grade;
                reviewCounts[teacherId] = (reviewCounts[teacherId] || 0) + 1;
            }
        });

        for (const teacherId in gradeSums) {
            if (reviewCounts[teacherId] > 0) {
                const average = gradeSums[teacherId] / reviewCounts[teacherId];
                ratingsMap[teacherId] = parseFloat(average.toFixed(1));
            }
        }
        return ratingsMap;
    };

    const fetchRatings = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await api.getReviews();
            const calculatedRatings = calculateAverageRatings(response.data);
            setRatings(calculatedRatings);
        } catch (err) {
            console.error("Failed to fetch reviews for ratings:", err);
            setError('Не удалось загрузить данные для расчета рейтингов.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRatings();
    }, [fetchRatings]);

    const value = useMemo(() => ({
        ratings,
        isLoading,
        error,
        fetchRatings
    }), [ratings, isLoading, error, fetchRatings]);

    return (
        <RatingContext.Provider value={value}>
            {children}
        </RatingContext.Provider>
    );
};