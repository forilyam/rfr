import {
    Avatar,
    Box,
    Card,
    CardContent,
    CardMedia,
    Chip,
    IconButton,
    ListItemIcon,
    Menu,
    MenuItem,
    Tooltip,
    Typography
} from '@mui/material';
import FavoriteRoundedIcon from '@mui/icons-material/FavoriteRounded';
import FavoriteBorderRoundedIcon from '@mui/icons-material/FavoriteBorderRounded';
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import {FC, MouseEvent, useContext, useState} from 'react';
import "./styles.css";
import {Photo} from '../../types/Photo';
import {SessionContext} from '../../context/SessionContext';
import {useDeletePhoto} from '../../hooks/useDeletePhoto';
import {useSnackBar} from '../../context/useSnackBar';
import {useLikePhoto} from '../../hooks/useLikePhoto';
import {errorMessage} from '../../api/graphqlError';

interface PhotoCardInterface {
    photo: Photo;
    onEditClick: (photo: Photo) => void;
}

export const PhotoCard: FC<PhotoCardInterface> = ({photo, onEditClick}) => {
    const {user} = useContext(SessionContext);
    const snackbar = useSnackBar();
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
    const liked = photo.likes?.likes?.some((el) => el.user === user?.id) ?? false;

    const {deletePhoto} = useDeletePhoto({
        onError: (e) => snackbar.showSnackBar(errorMessage(e, "Can not delete post"), "error"),
        onCompleted: () => snackbar.showSnackBar("Post deleted", "success"),
    });

    const {likePhoto} = useLikePhoto({
        onError: (e) => snackbar.showSnackBar(errorMessage(e, "Post was not liked"), "error"),
        onCompleted: () => snackbar.showSnackBar("Post was succesfully liked", "success"),
    });

    const openMenu = (event: MouseEvent<HTMLElement>) => setMenuAnchor(event.currentTarget);
    const closeMenu = () => setMenuAnchor(null);

    const handleEdit = () => {
        closeMenu();
        onEditClick(photo);
    };

    const handleDeletePhoto = () => {
        closeMenu();
        deletePhoto({
            variables: {
                id: photo.id,
            }
        });
    };

    const handleLikePhoto = () => {
        likePhoto({
            variables: {
                input: {
                    id: photo.id,
                    like: {
                        user: user?.id ?? "",
                    }
                }
            }
        });
    };

    return (
        <Card className="photo-card" sx={{height: "100%", display: "flex", flexDirection: "column"}}>
            <Box sx={{position: "relative", overflow: "hidden"}}>
                <CardMedia
                    component="img"
                    className="photo-card__image"
                    image={photo.src}
                    alt={photo.description || photo.country.name}
                />
                <Chip
                    size="small"
                    avatar={<Avatar src={photo.country.flag} alt=""/>}
                    label={photo.country.name}
                    sx={{
                        position: "absolute",
                        left: 12,
                        top: 12,
                        bgcolor: "background.paper",
                        boxShadow: 1,
                    }}
                />
                {photo.isOwner && (
                    <>
                        <Tooltip title="Actions">
                            <IconButton
                                aria-label="Photo actions"
                                size="small"
                                onClick={openMenu}
                                sx={{
                                    position: "absolute",
                                    right: 12,
                                    top: 10,
                                    bgcolor: "background.paper",
                                    boxShadow: 1,
                                    "&:hover": {bgcolor: "background.paper"},
                                }}
                            >
                                <MoreVertRoundedIcon fontSize="small"/>
                            </IconButton>
                        </Tooltip>
                        <Menu
                            anchorEl={menuAnchor}
                            open={Boolean(menuAnchor)}
                            onClose={closeMenu}
                            anchorOrigin={{vertical: "bottom", horizontal: "right"}}
                            transformOrigin={{vertical: "top", horizontal: "right"}}
                        >
                            <MenuItem onClick={handleEdit}>
                                <ListItemIcon><EditOutlinedIcon fontSize="small"/></ListItemIcon>
                                Edit
                            </MenuItem>
                            <MenuItem onClick={handleDeletePhoto} sx={{color: "error.main"}}>
                                <ListItemIcon sx={{color: "inherit"}}><DeleteOutlineRoundedIcon fontSize="small"/></ListItemIcon>
                                Delete
                            </MenuItem>
                        </Menu>
                    </>
                )}
            </Box>
            <CardContent sx={{flexGrow: 1, display: "flex", flexDirection: "column", gap: 1, pb: "12px !important"}}>
                <Typography variant="body2" className="photo-card__content" sx={{color: "text.secondary", minHeight: "2.86em"}}>
                    {photo.description}
                </Typography>
                <Box sx={{display: "flex", alignItems: "center", mt: "auto"}}>
                    <IconButton
                        aria-label="like"
                        size="small"
                        onClick={handleLikePhoto}
                        sx={{color: liked ? "error.main" : "text.secondary", ml: -0.75}}
                    >
                        {liked ? <FavoriteRoundedIcon fontSize="small"/> : <FavoriteBorderRoundedIcon fontSize="small"/>}
                    </IconButton>
                    <Typography variant="body2" sx={{fontWeight: 600}}>
                        {photo.likes.total} {photo.likes.total === 1 ? "like" : "likes"}
                    </Typography>
                </Box>
            </CardContent>
        </Card>
    );
};
